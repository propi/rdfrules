package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
sealed trait ExpandingRule extends Rule {
  //val headTriples: IndexedSeq[(Int, Int)]
  val maxVariable: Atom.Variable

  final def measures: TypedKeyMap.Immutable[Measure] = throw new UnsupportedOperationException

  final def headCoverage: Double = support.toDouble / headSize

  def headTriples(implicit thi: TripleIndex[Int]): Iterator[(Int, Int)] = (head.subject, head.`object`) match {
    case (_: Atom.Variable, _: Atom.Variable) =>
      thi.predicates(head.predicate).subjects.pairIterator.flatMap {
        case (s, oi) => oi.iterator.map(s -> _)
      }
    case (Atom.Constant(s), _: Atom.Variable) => thi.predicates(head.predicate).subjects(s).iterator.map(s -> _)
    case (_: Atom.Variable, Atom.Constant(o)) => thi.predicates(head.predicate).objects(o).iterator.map(_ -> o)
    case (Atom.Constant(s), Atom.Constant(o)) => Iterator(s -> o)
  }
}

object ExpandingRule {

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
    * Check two items
    *
    * @param atomItem1      atomItem1
    * @param atomItem2      atomItem2
    * @param variables      mapping for second rule
    * @param variablesItems variables which are objects of mapping
    * @return
    */
  private def checkAtomItemsEquality(atomItem1: Atom.Item, atomItem2: Atom.Item, variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]) = (atomItem1, atomItem2) match {
    case (Atom.Constant(value1), Atom.Constant(value2)) => (value1 == value2, variables, variablesItems)
    case (v1: Atom.Variable, v2: Atom.Variable) => variables.get(v2) match {
      //if item of the second rule is mapped then we return the object of mapping and we compare it with first rule item
      case Some(v2) => (v2 == v1, variables, variablesItems)
      //if item of the second rule is not mapped and the first rule item is some object of mapping we can not map the second item - it is duplicit mapping
      //e.g. variables(c -> b), variablesItems(b), we compare b with d, we can not map (d -> b), because b is also the object of mapping
      //in this case the comparison is always false
      case None if variablesItems(v1) => (false, variables, variablesItems)
      //if item of the second rule is not mapped and the first rule item is not some object of mapping we map second item to first item
      case _ => (true, variables + (v2 -> v1), variablesItems + v1)
    }
    case _ => (false, variables, variablesItems)
  }

  private def checkAtomEquality(atom1: Atom, atom2: Atom, variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]) = {
    if (atom1.predicate == atom2.predicate) {
      val (eqSubjects, v1, vi1) = checkAtomItemsEquality(atom1.subject, atom2.subject, variables, variablesItems)
      if (eqSubjects) {
        val (eqObjects, v2, vi2) = checkAtomItemsEquality(atom1.`object`, atom2.`object`, v1, vi1)
        (eqObjects, v2, vi2)
      } else {
        (false, variables, variablesItems)
      }
    } else {
      (false, variables, variablesItems)
    }
  }

  private def checkBodyEquality(body1: IndexedSeq[Atom], body2: Set[Atom], variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]): Boolean = {
    body1 match {
      case head +: tail =>
        body2.exists { atom =>
          val (eqAtoms, newVariables, newVariablesItems) = checkAtomEquality(head, atom, variables, variablesItems)
          eqAtoms && checkBodyEquality(tail, body2 - atom, newVariables, newVariablesItems)
        }
      case _ => body2.isEmpty
    }
  }

  private def checkPartialBodyEquality(bodyParent: IndexedSeq[Atom], bodyChild: Set[Atom], variables: Map[Atom.Variable, Atom.Variable], variablesItems: Set[Atom.Variable]): Boolean = {
    bodyParent match {
      case head +: tail if bodyChild.nonEmpty =>
        bodyChild.exists { atom =>
          val (eqAtoms, newVariables, newVariablesItems) = checkAtomEquality(head, atom, variables, variablesItems)
          eqAtoms && checkPartialBodyEquality(tail, bodyChild - atom, newVariables, newVariablesItems)
        }
      case _ => true
    }
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
    val (eqHeads, newVariables, newVariablesItems) = checkAtomEquality(head1, head2, Map.empty, Set.empty)
    eqHeads && checkBodyEquality(body1, body2, newVariables, newVariablesItems)
  }

  /**
    * Check parenthood
    *
    * @param bodyParent first list of body atoms
    * @param bodyChild  second list of body atoms
    * @param headParent first head
    * @param headChild  second head
    * @return boolean
    */
  def checkParenthood(bodyParent: IndexedSeq[Atom], bodyChild: Set[Atom], headParent: Atom, headChild: Atom): Boolean = {
    val (eqHeads, newVariables, newVariablesItems) = checkAtomEquality(headParent, headChild, Map.empty, Set.empty)
    eqHeads && checkPartialBodyEquality(bodyParent, bodyChild, newVariables, newVariablesItems)
  }

  case class ClosedRule(body: IndexedSeq[Atom], head: Atom)
                       (val support: Int,
                        val headSize: Int,
                        val variables: List[Atom.Variable],
                        val maxVariable: Atom.Variable) extends ExpandingRule {

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: ClosedRule => checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

  }

  case class DanglingRule(body: IndexedSeq[Atom], head: Atom)
                         (val support: Int,
                          val headSize: Int,
                          val variables: DanglingVariables,
                          val maxVariable: Atom.Variable) extends ExpandingRule {

    override def equals(obj: scala.Any): Boolean = obj match {
      case rule: DanglingRule if ruleLength == rule.ruleLength &&
        headSize == rule.headSize &&
        support == rule.support =>
        checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

  }

}