package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.utils.{MutableRanges, TypedKeyMap}

import scala.ref.SoftReference

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
sealed trait ExpandingRule extends Rule {
  val maxVariable: Atom.Variable

  def supportedRanges: Option[MutableRanges]

  final def measures: TypedKeyMap.Immutable[Measure] = throw new UnsupportedOperationException

  final def headCoverage: Double = support.toDouble / headSize

  def headTriples(injectiveMapping: Boolean)(implicit thi: TripleIndex[Int]): Iterator[(Int, Int)] = (head.subject, head.`object`) match {
    case (_: Atom.Variable, _: Atom.Variable) =>
      thi.predicates(head.predicate).subjects.pairIterator.flatMap {
        case (s, oi) => oi.iterator.filter(o => !injectiveMapping || s != o).map(s -> _)
      }
    case (Atom.Constant(s), _: Atom.Variable) => thi.predicates(head.predicate).subjects(s).iterator.filter(o => !injectiveMapping || s != o).map(s -> _)
    case (_: Atom.Variable, Atom.Constant(o)) => thi.predicates(head.predicate).objects(o).iterator.filter(s => !injectiveMapping || s != o).map(_ -> o)
    case (Atom.Constant(s), Atom.Constant(o)) => if (injectiveMapping && s == o) Iterator.empty else Iterator(s -> o)
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

  sealed trait ClosedRule extends ExpandingRule {
    override def equals(other: Any): Boolean = other match {
      case rule: ClosedRule if ruleLength == rule.ruleLength &&
        headSize == rule.headSize &&
        support == rule.support => checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

    //TODO check whether it is faster than Rule hashCode
    //override def hashCode(): Int = body.hashCode() * 31 + head.hashCode()

    val variables: List[Atom.Variable]
  }

  object ClosedRule {
    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: List[Atom.Variable], maxVariable: Atom.Variable): ClosedRule = new BasicClosedRule(body, head, support, headSize, variables, maxVariable)

    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: List[Atom.Variable], maxVariable: Atom.Variable, supportedRanges: MutableRanges): ClosedRule = new CachedClosedRule(body, head, support, headSize, variables, maxVariable, SoftReference(supportedRanges))
  }

  private class BasicClosedRule(val body: IndexedSeq[Atom],
                                val head: Atom,
                                val support: Int,
                                val headSize: Int,
                                val variables: List[Atom.Variable],
                                val maxVariable: Atom.Variable,
                               ) extends ClosedRule {
    def supportedRanges: Option[MutableRanges] = None
  }

  private class CachedClosedRule(val body: IndexedSeq[Atom],
                                 val head: Atom,
                                 val support: Int,
                                 val headSize: Int,
                                 val variables: List[Atom.Variable],
                                 val maxVariable: Atom.Variable,
                                 _supportedRanges: SoftReference[MutableRanges]
                                ) extends ClosedRule {
    def supportedRanges: Option[MutableRanges] = _supportedRanges.get
  }

  sealed trait DanglingRule extends ExpandingRule {
    override def equals(other: Any): Boolean = other match {
      case rule: DanglingRule if ruleLength == rule.ruleLength &&
        headSize == rule.headSize &&
        support == rule.support =>
        checkRuleContentsEquality(body, rule.body.toSet, head, rule.head)
      case _ => false
    }

    //TODO check whether it is faster than Rule hashCode
    //override def hashCode(): Int = (1 + body.hashCode()) * 31 + head.hashCode()

    val variables: DanglingVariables
  }

  object DanglingRule {
    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: DanglingVariables, maxVariable: Atom.Variable): DanglingRule = new BasicDanglingRule(body, head, support, headSize, variables, maxVariable)

    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: DanglingVariables, maxVariable: Atom.Variable, supportedRanges: MutableRanges): DanglingRule = new CachedDanglingRule(body, head, support, headSize, variables, maxVariable, SoftReference(supportedRanges))
  }

  private class BasicDanglingRule(val body: IndexedSeq[Atom],
                                  val head: Atom,
                                  val support: Int,
                                  val headSize: Int,
                                  val variables: DanglingVariables,
                                  val maxVariable: Atom.Variable) extends DanglingRule {
    def supportedRanges: Option[MutableRanges] = None
  }

  private class CachedDanglingRule(val body: IndexedSeq[Atom],
                                   val head: Atom,
                                   val support: Int,
                                   val headSize: Int,
                                   val variables: DanglingVariables,
                                   val maxVariable: Atom.Variable,
                                   _supportedRanges: SoftReference[MutableRanges]
                                  ) extends DanglingRule {
    def supportedRanges: Option[MutableRanges] = _supportedRanges.get
  }

}