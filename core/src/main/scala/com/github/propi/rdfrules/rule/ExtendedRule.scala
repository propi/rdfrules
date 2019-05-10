package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
sealed trait ExtendedRule extends Rule {
  val headTriples: IndexedSeq[(Int, Int)]
  val maxVariable: Atom.Variable
  val patterns: List[RulePattern.Mapped]
  val measures: TypedKeyMap[Measure]

  def headSize: Int = measures(Measure.HeadSize).value

  def withPatterns(patterns: List[RulePattern.Mapped]): ExtendedRule
}

object ExtendedRule {

  sealed trait DanglingVariables {
    def others: List[Atom.Variable]

    def danglings: List[Atom.Variable]
  }

  case class OneDangling(dangling: Atom.Variable, others: List[Atom.Variable]) extends DanglingVariables {
    def danglings: List[Atom.Variable] = List(dangling)
  }

  case class TwoDanglings(dangling1: Atom.Variable, dangling2: Atom.Variable, others: List[Atom.Variable]) extends DanglingVariables {
    def danglings: List[Atom.Variable] = List(dangling1, dangling2)
  }

  /**
    * Check whether two lists of atoms are isomorphic
    *
    * @param body1 first list of body atoms
    * @param body2 second list of body atoms
    * @param head1 first head
    * @param head2 second head
    * @return boolean
    */
  def checkRuleContentsEquality(body1: IndexedSeq[Atom], body2: Set[Atom], head1: Atom, head2: Atom): Boolean = {
    def checkAtomItemsEquality(atomItem1: Atom.Item, atomItem2: Atom.Item, variables: Map[Atom.Variable, Atom.Variable]) = (atomItem1, atomItem2) match {
      case (Atom.Constant(value1), Atom.Constant(value2)) if value1 == value2 => true -> variables
      case (v1: Atom.Variable, v2: Atom.Variable) =>
        val nv = variables + (v2 -> variables.getOrElse(v2, v1))
        (nv(v2) == v1) -> nv
      case _ => false -> variables
    }

    def checkAtomEquality(atom1: Atom, atom2: Atom, variables: Map[Atom.Variable, Atom.Variable]) = {
      if (atom1.predicate == atom2.predicate) {
        val (eqSubjects, v1) = checkAtomItemsEquality(atom1.subject, atom2.subject, variables)
        if (eqSubjects) {
          val (eqObjects, v2) = checkAtomItemsEquality(atom1.`object`, atom2.`object`, v1)
          eqObjects -> v2
        } else {
          false -> variables
        }
      } else {
        false -> variables
      }
    }

    def checkBodyEquality(body1: IndexedSeq[Atom], body2: Set[Atom], variables: Map[Atom.Variable, Atom.Variable]): Boolean = {
      body1 match {
        case head +: tail =>
          body2.exists { atom =>
            val (eqAtoms, newVariables) = checkAtomEquality(head, atom, variables)
            eqAtoms && checkBodyEquality(tail, body2 - atom, newVariables)
          }
        case _ => body2.isEmpty
      }
    }

    val (eqHeads, newVariables) = checkAtomEquality(head1, head2, Map.empty)
    eqHeads && checkBodyEquality(body1, body2, newVariables)
  }

  case class ClosedRule(body: IndexedSeq[Atom], head: Atom)
                       (val measures: TypedKeyMap[Measure],
                        val patterns: List[RulePattern.Mapped],
                        val variables: List[Atom.Variable],
                        val maxVariable: Atom.Variable,
                        val headTriples: IndexedSeq[(Int, Int)]) extends ExtendedRule {

    def withPatterns(patterns: List[RulePattern.Mapped]): ExtendedRule = this.copy()(measures, patterns, variables, maxVariable, headTriples)

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: ClosedRule => checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

  }

  case class DanglingRule(body: IndexedSeq[Atom], head: Atom)
                         (val measures: TypedKeyMap[Measure],
                          val patterns: List[RulePattern.Mapped],
                          val variables: DanglingVariables,
                          val maxVariable: Atom.Variable,
                          val headTriples: IndexedSeq[(Int, Int)]) extends ExtendedRule {

    def withPatterns(patterns: List[RulePattern.Mapped]): ExtendedRule = this.copy()(measures, patterns, variables, maxVariable, headTriples)

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: DanglingRule => checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

  }

}