package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.rule.Atom
import RuleExpansion._

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait RuleFilter {

  def apply(newAtom: Atom, support: Int): Boolean

  def isDefined = true

  final def &(filter: RuleFilter): RuleFilter = if (filter.isDefined) new RuleFilter.And(this, filter) else this

}

object RuleFilter {

  class And(ruleFilter1: RuleFilter, ruleFilter2: RuleFilter) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = ruleFilter1(newAtom, support) && ruleFilter2(newAtom, support)
  }

  class MinSupportRuleFilter(minSupport: Double) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = support >= minSupport
  }

  class NoDuplicitRuleFilter(head: Atom, body: Set[Atom]) extends RuleFilter {
    def apply(newAtom: Atom, support: Int): Boolean = newAtom != head && !body(newAtom)
  }

  class NoRepeatedGroups(withDuplicitPredicates: Boolean,
                         atoms: Set[Atom],
                         rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition, collection.Seq[Atom.Item]]]) extends RuleFilter {

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
      newAtom.subject.isInstanceOf[Atom.Variable] &&
        newAtom.`object`.isInstanceOf[Atom.Variable] &&
        rulePredicates.get(newAtom.predicate).exists { implicit p =>
          implicit val replacement: TripleItemPosition => Atom.Item = {
            case _: TripleItemPosition.Subject => newAtom.`object`
            case _: TripleItemPosition.Object => newAtom.subject
          }
          hasRedundantComplement(newAtom.subjectPosition) || hasRedundantComplement(newAtom.objectPosition)
        }
      )

  }

}
