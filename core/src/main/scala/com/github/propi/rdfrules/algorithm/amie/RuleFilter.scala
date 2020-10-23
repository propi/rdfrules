package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.extensions.EitherExtension._

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

  type FilterResult = (Boolean, Option[ExtendedRule => ExtendedRule])

  private implicit def booleanToFilterResult(bool: Boolean): FilterResult = bool -> None

  class And(ruleFilter1: RuleFilter, ruleFilter2: RuleFilter) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = {
      val rf1 = ruleFilter1(newAtom, support)
      val rf2 = ruleFilter2(newAtom, support)
      val f = (rf1._2, rf2._2) match {
        case (Some(f), Some(g)) => Some(f.andThen(g))
        case (x, y) => x.orElse(y)
      }
      (rf1._1 && rf2._1) -> f
    }
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
  class RulePatternFilter(rule: ExtendedRule)(implicit thi: TripleIndex[Int], atomMatcher: AtomPatternMatcher[Atom], freshAtomMatcher: AtomPatternMatcher[FreshAtom]) extends RuleFilter {
    /**
      * Patterns for remaining fresh atoms which can be added to this rule
      */
    private val patternAtoms = rule.patterns.map(x => x -> x.antecedent.lift(x.antecedent.size - rule.body.size - 1))

    /**
      * Check whether a new atom/fresh atom is matching with the current atom pattern
      *
      * @param atom new atom
      * @return boolean
      */
    private def matchAtom(atom: Either[Atom, FreshAtom]): Iterator[RulePattern.Mapped] = patternAtoms.iterator.filter(x => (x._2.isEmpty && !x._1.exact) || x._2.exists(y => atom.fold(atomMatcher.matchPattern(_, y), freshAtomMatcher.matchPattern(_, y)))).map(_._1)

    /**
      * this rule is defined only if
      *  - rule pattern must not be empty and
      *  - there is any partial pattern or nonEmpty atom pattern (if there are only exact patterns and are completely matched then we will not apply this filter)
      *
      * @return boolean
      */
    override val isDefined: Boolean = rule.patterns.nonEmpty && patternAtoms.exists { case (rp, ap) => !rp.exact || ap.nonEmpty }

    def apply(newAtom: Atom, support: Int): FilterResult = {
      val matchedPatterns = matchAtom(newAtom).toList
      matchedPatterns.nonEmpty -> Some(_.withPatterns(matchedPatterns))
    }

    def matchFreshAtom(freshAtom: FreshAtom): Boolean = rule.patterns.isEmpty || matchAtom(freshAtom).nonEmpty
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
  class NoDuplicitRuleFilter(head: Atom, body: Set[Atom]) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): FilterResult = {
      val x = newAtom != head && !body(newAtom)
      if (!x) println(x)
      x
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
