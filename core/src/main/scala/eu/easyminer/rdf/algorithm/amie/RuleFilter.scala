package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.rule.{Atom, TripleItemPosition}

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
  def apply(newAtom: Atom, support: Int): Boolean

  def isDefined = true

  final def &(filter: RuleFilter): RuleFilter = if (filter.isDefined) new RuleFilter.And(this, filter) else this

}

object RuleFilter {

  class And(ruleFilter1: RuleFilter, ruleFilter2: RuleFilter) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = ruleFilter1(newAtom, support) && ruleFilter2(newAtom, support)
  }

  /**
    * Filter all atoms which have support greater or equal than min support
    *
    * @param minSupport min support threshold
    */
  class MinSupportRuleFilter(minSupport: Double) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = support >= minSupport
  }

  /**
    * Filter all atoms which are already contained in the rule
    *
    * @param head head of the rule
    * @param body body of the rule
    */
  class NoDuplicitRuleFilter(head: Atom, body: Set[Atom]) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = newAtom != head && !body(newAtom)
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
  class NoRepeatedGroups(withDuplicitPredicates: Boolean,
                         atoms: Set[Atom],
                         rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition, collection.Seq[Atom.Item]]]) extends RuleFilter {

    /**
      * We apply this filter only if we have allowed duplicit predicates and number of atoms is greater or equal 3
      *
      * @return true = filter is defined
      */
    override def isDefined: Boolean = withDuplicitPredicates && atoms.size >= 3

    private def replaceItemInAtom(old: Atom.Item, replacement: Atom.Item)(atom: Atom) = {
      if (atom.subject == old) atom.copy(subject = replacement)
      else if (atom.`object` == old) atom.copy(`object` = replacement)
      else atom
    }

    private def hasRedundantComplement(itemPosition: TripleItemPosition)
                                      (implicit
                                       itemPositionToReplacement: TripleItemPosition => Atom.Item,
                                       p: collection.Map[TripleItemPosition, collection.Seq[Atom.Item]]) = {
      val replacement = itemPositionToReplacement(itemPosition)
      p.get(itemPosition).exists(_.exists(o => atoms.map(replaceItemInAtom(o, replacement)).size < atoms.size))
    }

    def apply(newAtom: Atom, support: Int): Boolean = !(
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
          implicit val replacement: TripleItemPosition => Atom.Item = {
            case _: TripleItemPosition.Subject => newAtom.`object`
            case _: TripleItemPosition.Object => newAtom.subject
          }
          hasRedundantComplement(newAtom.subjectPosition) || hasRedundantComplement(newAtom.objectPosition)
        }
      )

  }

}
