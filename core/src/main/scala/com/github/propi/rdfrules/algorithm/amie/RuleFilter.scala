package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{TypedKeyMap, Wrapper}

import scala.annotation.tailrec
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */

/**
  * Filter of new atoms which want to be added to a rule
  */
trait RuleFilter {

  /**
    * Check whether a new atom can be added to a rule
    *
    * @param newAtom new atom to be added
    * @param support new support for the rule with the new atom
    * @return true = atom can be added
    */
  def apply(newAtom: Atom, support: Int): RuleFilter.FilterResult

  def isDefined = true

  final def &(filter: RuleFilter): RuleFilter = if (filter.isDefined) new RuleFilter.And(this, filter) else this

}

object RuleFilter {

  type FilterResult = Boolean

  //private implicit def booleanToFilterResult(bool: Boolean): FilterResult = bool -> None

  class And(ruleFilter1: RuleFilter, ruleFilter2: RuleFilter) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = ruleFilter1(newAtom, support) && ruleFilter2(newAtom, support)
  }

  class RuleConstraints(rule: Rule, constraints: Seq[RuleConstraint.MappedFilter]) extends RuleFilter {
    /**
      * Check whether a new atom can be added to a rule
      *
      * @param newAtom new atom to be added
      * @param support new support for the rule with the new atom
      * @return true = atom can be added
      */
    def apply(newAtom: Atom, support: Int): FilterResult = constraints.forall(_.test(newAtom, Some(rule)))

    override def isDefined: Boolean = constraints.nonEmpty
  }

  /**
    * Filter atoms by rule patterns.
    * If the rule patterns collections is empty or all paterns all exact and matched then this filter is not defined.
    * This filter also replace old rule patterns by matched patterns within new rule
    *
    * @param rule original rule (without refinement)
    */
  class RulePatternFilter(rule: ExpandingRule, patterns: List[RulePattern.Mapped], maxRuleLength: Int)(implicit atomMatcher: MappedAtomPatternMatcher[Atom], freshAtomMatcher: MappedAtomPatternMatcher[FreshAtom], tripleItemIndex: TripleItemIndex) extends RuleFilter {

    private def _matchAtom(atom: Either[Atom, FreshAtom], patterns: Iterable[(AtomPattern.Mapped, Aliases)]): Boolean = atom.fold(atom => patterns.exists(x => atomMatcher.matchPattern(atom, x._1)(x._2).isDefined), freshAtom => patterns.exists(x => freshAtomMatcher.matchPattern(freshAtom, x._1)(x._2).isDefined))

    private def lowHighPermutations[A, B](higherCol: Set[A], loverCol: Iterable[B]): Iterator[List[(A, B)]] = {
      if (loverCol.isEmpty) {
        Iterator(Nil)
      } else {
        val head = loverCol.head
        val tail = loverCol.tail
        higherCol.iterator.flatMap(x => lowHighPermutations(higherCol - x, tail).map((x -> head) :: _))
      }
    }

    private def permutations[A, B](col1: Iterable[A], col2: Iterable[B]): Iterator[Iterable[(A, B)]] = {
      if (col1.size >= col2.size) {
        lowHighPermutations(col1.toSet, col2)
      } else {
        lowHighPermutations(col2.toSet, col1).map(_.view.map(_.swap))
      }
    }

    private sealed trait NewAtomMatcher {
      def matchAtom(atom: Either[Atom, FreshAtom]): Boolean

      def isDefined: Boolean
    }

    /**
      * The main object here is remainingAtomPatterns. This set constains all non matched atoms.
      * This matcher is defined only if remainingAtomPatterns has some elements (and rule.body < pattern.body)
      * A fresh atom must match one of remainingAtomPatterns.
      *
      * @param pattern rule pattern
      */
    private class OrderlessExactAtomMatcher(pattern: RulePattern.Mapped)(implicit aliases: Aliases) extends NewAtomMatcher {
      private val remainingAtomPatterns: Set[(AtomPattern.Mapped, Aliases)] = {
        if (rule.body.length < pattern.body.length) {
          def selectRemainingAtomPatterns(body: Seq[Atom], atomPatterns: Set[Wrapper[AtomPattern.Mapped]])(implicit aliases: Aliases): Set[(AtomPattern.Mapped, Aliases)] = {
            if (body.isEmpty) {
              atomPatterns.map(_.x -> aliases)
            } else {
              //each atom of the rule must match an atom pattern since it must be exact
              //we start from the left-side atom to the right-side atom.
              val x = body.head
              val tail = body.tail
              //for each atom we find all matching paterns and for each we create an individual branch
              atomPatterns
                .iterator
                .flatMap(y => atomMatcher.matchPattern(x, y.x).map(y -> _))
                .map { case (pattern, aliases) => selectRemainingAtomPatterns(tail, atomPatterns - pattern)(aliases) }
                .reduceOption(_ ++ _)
                .getOrElse(Set.empty)
            }
          }

          selectRemainingAtomPatterns(rule.body, pattern.body.iterator.map(Wrapper(_)).toSet)
        } else {
          Set.empty
        }
      }

      def isDefined: Boolean = remainingAtomPatterns.nonEmpty

      def matchAtom(atom: Either[Atom, FreshAtom]): Boolean = _matchAtom(atom, remainingAtomPatterns)
    }

    /**
      * The main object here is nextAtomPattern. This optional value constains the optional next pattern to be matching for a fresh atom.
      * This matcher is defined only if nextAtomPattern is defined - nextAtomPattern is:
      *  - None = the rule is not matching the pattern
      *  - Some(None) = the rule is matching the pattern and there are no other atom pattern to be matched (the rule has been completely matched). But the pattern is partial, therefore, the next fresh atom can be anything.
      *  - Some(Some(..)) = the next atom pattern which must be matched by a fresh atom.
      *
      * @param pattern rule pattern
      */
    private class GradualPartialAtomMatcher(pattern: RulePattern.Mapped)(implicit aliases: Aliases) extends NewAtomMatcher {
      protected val nextAtomPattern: Option[Option[(AtomPattern.Mapped, Aliases)]] = {
        val isMatched = rule.body.reverseIterator.zip(pattern.body.reverseIterator).foldLeft(Option(aliases))((res, x) => res.flatMap { implicit aliases =>
          atomMatcher.matchPattern(x._1, x._2)
        })
        isMatched.map { aliases =>
          if (rule.body.length < pattern.body.length) {
            Some(pattern.body(pattern.body.length - rule.body.length - 1) -> aliases)
          } else {
            None
          }
        }
      }

      def isDefined: Boolean = nextAtomPattern.isDefined

      def matchAtom(atom: Either[Atom, FreshAtom]): Boolean = nextAtomPattern.exists(_.forall(x => _matchAtom(atom, Iterable(x))))
    }

    /**
      * The behaviour is same as GradualPartialAtomMatcher, but this matcher is defined only if nextAtomPattern is Some(Some(..))
      *
      * @param pattern rule pattern
      */
    private class GradualExactAtomMatcher(pattern: RulePattern.Mapped)(implicit aliases: Aliases) extends GradualPartialAtomMatcher(pattern) {
      override def isDefined: Boolean = nextAtomPattern.flatten.isDefined

      override def matchAtom(atom: Either[Atom, FreshAtom]): Boolean = {
        val _nextAtomPattern = nextAtomPattern.flatten
        _matchAtom(atom, _nextAtomPattern)
      }
    }

    /**
      * The main object here is remainingAtomPatterns and minRequiredMatching.
      * minRequiredMatching is the K value telling us that in the current refining process we need to match minimal K patterns by with rule atoms including a fresh atom.
      * For example:
      *  - minRequiredMatching = 0: the fresh atom need not match any pattern (e.g. maxRuleLength = 3, rule is () => (?a p ?b) and pattern is (? ? ?) => (? ? ?))
      *  - minRequiredMatching = 1: the fresh atom must match minimal one pattern if no rules matching any pattern (e.g. maxRuleLength = 3, rule is (?a p ?b) => (?a p ?b) and pattern is (? ? ?) => (? ? ?)).
      *  - minRequiredMatching > 1: the fresh atom must match minimal one pattern if min one pattern is matching with a rule atom (e.g. maxRuleLength = 3, rule is (?a p ?b) => (?a p ?b) and pattern is (? ? ?) & (? ? ?) => (? ? ?)).
      *
      * remainingAtomPatterns has these variants:
      *  - None = the fresh atom can be anything
      *  - Some(Nil) = this matcher is not defined because the rule can not match this pattern
      *  - Some(Col) = the fresh atom must match one of patterns in the Col.
      *
      * @param pattern rule pattern
      */
    private class OrderlessPartialAtomMatcher(pattern: RulePattern.Mapped)(implicit aliases: Aliases) extends NewAtomMatcher {
      private val minRequiredMatching = pattern.body.length - maxRuleLength + rule.body.length + 2

      private val remainingAtomPatterns: Option[Iterable[(AtomPattern.Mapped, Aliases)]] = {
        if (minRequiredMatching <= 0) {
          None
        } else if (minRequiredMatching == 1) {
          val allIsMatched = pattern.body.exists(atomPattern => rule.body.exists(atom => atomMatcher.matchPattern(atom, atomPattern).isDefined))
          if (allIsMatched) {
            //if minRequiredMatching is 1 and some atom is matching any pattern then the fresh atom can be anything (because the rule pattern is satified).
            None
          } else {
            //otherwise the fresh atom must match one of atom patterns
            Some(pattern.body.view.map(_ -> aliases))
          }
        } else {
          //only if minRequiredMatching >= 2
          //Rule-pattern permutation, e.g, p1, p2 (atom patterns), a1, a2, a3 (rule atoms)
          //- perms are: (p1:a1, p2:a2), (p1:a1, p2:a3), (p1:a2, p2:a1), (p1:a2, p2:a3), (p1:a3, p2:a1), (p1:a3, p2:a2)
          //or, e.g, p1, p2, p3 (atom patterns), a1, a2 (rule atoms)
          //- perms are: (a1:p1, a2:p2), (a1:p1, p2:p3), (a1:p2, a2:p1), (a1:p2, a2:p3), (a1:p3, a2:p1), (a1:p3, a2:p2)
          val bodyPatterns = pattern.body.iterator.map(Wrapper(_)).toSet
          collectNonMatchedPatterns(permutations(bodyPatterns, rule.body), Set.empty, bodyPatterns)
        }
      }

      @tailrec
      private def collectNonMatchedPatterns(it: Iterator[Iterable[(Wrapper[AtomPattern.Mapped], Atom)]], res: Set[(AtomPattern.Mapped, Aliases)], bodyPatterns: Set[Wrapper[AtomPattern.Mapped]]): Option[Set[(AtomPattern.Mapped, Aliases)]] = {
        if (it.hasNext) {
          //get a permutation, e.g. (p1:a1, p2:a2)
          val perm = it.next()
          //we count all matched patterns
          //we return all pattern which are matching with the paired atom of the permutation
          val matchedPatterns = collection.mutable.Set.empty[Wrapper[AtomPattern.Mapped]]
          val filledAliases = perm.foldLeft(aliases) { case (aliases, (atomPattern, atom)) =>
            atomMatcher.matchPattern(atom, atomPattern.x)(aliases) match {
              case Some(aliases) =>
                matchedPatterns += atomPattern
                aliases
              case None => aliases
            }
          }
          //we recompute minRequiredMatching without matched patterns
          val newMinRequiredMatching = minRequiredMatching - matchedPatterns.size
          if (newMinRequiredMatching <= 0) {
            //all atoms are matching patterns therefore the rule is completely matched and the fresh atom can be anything
            None
          } else if (newMinRequiredMatching == 1) {
            //only one atom pattern must be matched by the fresh atom, we save all nonMatchedPatterns which should be matched by the fresh atom (min one of them)
            collectNonMatchedPatterns(it, res ++ bodyPatterns.iterator.filterNot(matchedPatterns).map(_.x -> filledAliases), bodyPatterns)
          } else {
            //still more than one atom pattern must be matched. But now it is not possible to do because the fresh atom can match only one pattern
            //therefore this permutation is invalid and skipped.
            collectNonMatchedPatterns(it, res, bodyPatterns)
          }
        } else {
          Some(res)
        }
      }

      def isDefined: Boolean = remainingAtomPatterns.forall(_.nonEmpty)

      def matchAtom(atom: Either[Atom, FreshAtom]): Boolean = remainingAtomPatterns.forall(_matchAtom(atom, _))
    }

    /**
      * for each pattern create a new atom matcher which must be defined (valid pattern regarding the rule)
      */
    private val newAtomMatchers: List[NewAtomMatcher] = {
      patterns.iterator.flatMap(pattern => pattern.head match {
        case Some(head) => atomMatcher.matchPattern(rule.head, head)(Aliases.empty).map(pattern -> _)
        case None => Some(pattern -> Aliases.empty)
      }).flatMap { case (pattern, aliases) =>
        implicit val _aliases: Aliases = aliases
        val newAtomMatcher = (pattern.orderless, pattern.exact) match {
          case (true, true) => new OrderlessExactAtomMatcher(pattern)
          case (false, true) => new GradualExactAtomMatcher(pattern)
          case (false, false) => new GradualPartialAtomMatcher(pattern)
          case (true, false) => new OrderlessPartialAtomMatcher(pattern)
        }
        if (newAtomMatcher.isDefined) Some(newAtomMatcher) else None
      }.toList
    }

    /**
      * this rule is defined only if there is a defined new atom matcher (from rule patterns)
      *
      * @return boolean
      */
    override val isDefined: Boolean = newAtomMatchers.nonEmpty

    def apply(newAtom: Atom, support: Int): FilterResult = {
      newAtomMatchers.exists { atomMatcher =>
        atomMatcher.matchAtom(Left(newAtom))
      }
    }

    def matchFreshAtom(freshAtom: FreshAtom): Boolean = {
      !isDefined || newAtomMatchers.exists { atomMatcher =>
        atomMatcher.matchAtom(Right(freshAtom))
      }
    }
  }

  /**
    * Filter all atoms which have support greater or equal than min support
    *
    * @param minSupport min support threshold
    */
  class MinSupportRuleFilter(minSupport: Double) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = support >= minSupport
  }

  /**
    * Filter all atoms which are already contained in the rule
    * TODO - check if it is ever used
    *
    * @param head head of the rule
    * @param body body of the rule
    */
  @deprecated("This is resolved by variables mapping and duplicate checking.", "1.4.3")
  class NoDuplicitRuleFilter(head: Atom, body: Set[Atom]) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = {
      val res = newAtom != head && !body(newAtom)
      /*if (!res) {
        println(s"$newAtom : $body => $head")
      }*/
      res
      //val x = newAtom != head && !body(newAtom)
      //if (!x) println(s"atom: $newAtom, rule $body -> $head")
      //x
    }
  }

  /**
    * Filter no relevant fresh atoms, e.g., (a p C) & (a p1 b) => (a p b)
    * If (a p C) is mapped only to one triple then it is quasi binding and is skipped
    * IMPORTANT: this does not filter all quasi binding rules, only fresh atom.
    * For example: (a p C) & (a p1 b) => (a p b) IS OK, if we add new atom (b p C) & (a p C) & (a p1 b) => (a p b) then
    * (b p C) does not need to be quasi binding but (a p C) is now quasi binding!!! This is not checked in this place. Use hasQuasiBinding function on mined rules!
    *
    * @param body             body atoms
    * @param injectiveMapping injective mapping
    * @param atomCounting     Atom Counting
    */
  class QuasiBindingFilter(body: Set[Atom], injectiveMapping: Boolean, atomCounting: AtomCounting) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = {
      if (newAtom.subject.isInstanceOf[Atom.Constant] || newAtom.`object`.isInstanceOf[Atom.Constant]) {
        atomCounting.countDistinctPairs(body + newAtom, Atom(newAtom.subject, atomCounting.tripleItemIndex.zero, newAtom.`object`), 1.0, injectiveMapping) > 1
      } else {
        true
      }
    }
  }

  /**
    * If we add an atom to a rule which has 3 or more atoms, than new rule can contain isomorphic groups of items with various variables.
    * It is problem because it can not decrease support any more and the new atom which causes this problem is redundant!
    * E.g. we have this rule p(c,d) p(c,b) -> p(a,b) then we can create this rule p(a,d) p(c,d) p(c,b) -> p(a,b)
    * - this rule has two isomorphic groups {p(a,d) p(c,d)} and {p(a,b) p(c,b)}
    * - we want to filter all rules which contains isomophic groups because there is no additional information - only duplicit
    *
    * @param withDuplicitPredicates flag which indicates whether duplicit predicates are allowed
    * @param atoms                  all atoms in the rule
    * @param rulePredicates         all predicates of the rule which are mapped to items Map(subject -> list(objects)) AND Map(object -> list(subjects))
    */
  @deprecated("This is resolved by variables mapping because it is banned to have two identical instantiated atoms in the rule.", "1.4.3")
  class NoRepeatedGroups(withDuplicitPredicates: Boolean,
                         atoms: Set[Atom],
                         rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition[Atom.Item], collection.Seq[Atom.Item]]]) extends RuleFilter {

    /**
      * We apply this filter only if we have allowed duplicit predicates and number of atoms is greater or equal 3
      *
      * @return true = filter is defined
      */
    override def isDefined: Boolean = withDuplicitPredicates && atoms.size >= 3

    private def replaceItemInAtom(old: Atom.Item, replacement: Atom.Item)(atom: Atom) = {
      if (atom.subject == old) atom.transform(subject = replacement)
      else if (atom.`object` == old) atom.transform(`object` = replacement)
      else atom
    }

    private def hasRedundantComplement(itemPosition: TripleItemPosition[Atom.Item])
                                      (implicit
                                       itemPositionToReplacement: TripleItemPosition[Atom.Item] => Atom.Item,
                                       p: collection.Map[TripleItemPosition[Atom.Item], collection.Seq[Atom.Item]]) = {
      val replacement = itemPositionToReplacement(itemPosition)
      p.get(itemPosition).exists(_.exists(o => atoms.map(replaceItemInAtom(o, replacement)).size < atoms.size))
    }

    //TODO it works only for variables. It should work also for constants
    //For this variant it should filter: ( ?c <direction> "east" ) ^ ( ?b <train_id> ?c ) ^ ( ?b <train_id> ?a ) ⇒ ( ?a <direction> "east" )
    def apply(newAtom: Atom, support: Int): FilterResult = !(
      //we want to detect isomorphic group - if it exists then return true and then negate it - result is false
      //only variable atoms can cause isomorphic problem
      //and only if there is some atom with the same predicate
      newAtom.subject.isInstanceOf[Atom.Variable] &&
        newAtom.`object`.isInstanceOf[Atom.Variable] &&
        rulePredicates.get(newAtom.predicate).exists { implicit p =>
          //rule is: p(c,d) p(c,b) -> p(a,b)
          //new atom is: p(a,d)
          //we found all atoms which have same predicate as the new atom: { p(c,d) p(c,b), p(a,b) }
          //then from these atoms we filter those which have same subject item as new atom: p(a,b)
          //then from each of filtered atoms take its object item: b
          //then we replace these object items by object item in new atom in all atoms: replace b item by d => p(c,d) p(c,d) -> p(a,d)
          //if there are some duplicit atoms, then we found isomorphic group and we return true
          //do same for object item!
          implicit val replacement: TripleItemPosition[Atom.Item] => Atom.Item = {
            case TripleItemPosition.Subject(_) => newAtom.`object`
            case TripleItemPosition.Object(_) => newAtom.subject
          }
          hasRedundantComplement(newAtom.subjectPosition) || hasRedundantComplement(newAtom.objectPosition)
        }
      )

  }

}
