package eu.easyminer.rdf.rule

import eu.easyminer.rdf.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
sealed trait ExtendedRule extends Rule {

  val headTriples: IndexedSeq[(Int, Int)]
  val maxVariable: Atom.Variable
  val patterns: List[RulePattern]

  def headSize: Int = headTriples.length

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
    * @param body1     first list of atoms
    * @param body2     second list of atoms
    * @param variables mapping for variables from body2 which are mapped to variables from body1
    * @return boolean
    */
  def checkBodyEquality(body1: IndexedSeq[Atom], body2: Set[Atom], variables: Map[Atom.Variable, Atom.Variable] = Map.empty): Boolean = {
    def checkAtomItemsEquality(atomItem1: Atom.Item, atomItem2: Atom.Item, variables: Map[Atom.Variable, Atom.Variable]) = (atomItem1, atomItem2) match {
      case (Atom.Constant(value1), Atom.Constant(value2)) if value1 == value2 => true -> variables
      case (v1: Atom.Variable, v2: Atom.Variable) =>
        val nv = variables + (v2 -> variables.getOrElse(v2, v1))
        (nv(v2) == v1) -> nv
      case _ => false -> variables
    }

    def checkAtomEquality(atom1: Atom, atom2: Atom) = {
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

    body1 match {
      case head +: tail =>
        body2.exists { atom =>
          val (eqAtoms, variables) = checkAtomEquality(head, atom)
          eqAtoms && checkBodyEquality(tail, body2 - atom, variables)
        }
      case _ => body2.isEmpty
    }
  }

  case class ClosedRule(body: IndexedSeq[Atom], head: Atom)
                       (val measures: TypedKeyMap[Measure],
                        val patterns: List[RulePattern],
                        val variables: List[Atom.Variable],
                        val maxVariable: Atom.Variable,
                        val headTriples: IndexedSeq[(Int, Int)]) extends ExtendedRule {

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: ClosedRule => checkBodyEquality(body, rule.body.toSet)
      case _ => false
    }

  }

  case class DanglingRule(body: IndexedSeq[Atom], head: Atom)
                         (val measures: TypedKeyMap[Measure],
                          val patterns: List[RulePattern],
                          val variables: DanglingVariables,
                          val maxVariable: Atom.Variable,
                          val headTriples: IndexedSeq[(Int, Int)]) extends ExtendedRule {

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: DanglingRule => checkBodyEquality(body, rule.body.toSet)
      case _ => false
    }

  }

}