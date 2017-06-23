package eu.easyminer.rdf.rule

import eu.easyminer.rdf.utils.HowLong

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule[T <: Iterable[Atom] {def reverse : T}] {
  val body: T
  val head: Atom
  val headTriples: List[(String, String)]
  val measures: Measure.Measures
  val ruleLength: Int
  val maxVariable: Atom.Variable

  def headSize = headTriples.length
}


object Rule {

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

  def checkBodyEquality(body1: List[Atom], body2: Set[Atom], variables: Map[Atom.Variable, Atom.Variable] = Map.empty): Boolean = {
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
      case head :: tail =>
        body2.exists { atom =>
          val (eqAtoms, variables) = checkAtomEquality(head, atom)
          eqAtoms && checkBodyEquality(tail, body2 - atom, variables)
        }
      case _ => body2.isEmpty
    }
  }

}

case class ClosedRule(body: List[Atom], head: Atom)
                     (val measures: Measure.Measures,
                      val variables: List[Atom.Variable],
                      val maxVariable: Atom.Variable,
                      val headTriples: List[(String, String)]) extends Rule[List[Atom]] {
  lazy val ruleLength: Int = body.length + 1

  override def hashCode(): Int = {
    val support = measures(Measure.Support).asInstanceOf[Measure.Support].value
    val headSize = measures(Measure.HeadSize).asInstanceOf[Measure.HeadSize].value
    body.size * headSize + support
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case rule: ClosedRule => Rule.checkBodyEquality(body, rule.body.toSet)
    case _ => false
  }

  override def toString: String = body.mkString(" ^ ") + " -> " + head + "  :  " + " support:" + measures(Measure.Support).asInstanceOf[Measure.Support].value + ", hc:" + measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value
}

case class DanglingRule(body: List[Atom], head: Atom)
                       (val measures: Measure.Measures,
                        val variables: Rule.DanglingVariables,
                        val maxVariable: Atom.Variable,
                        val headTriples: List[(String, String)]) extends Rule[List[Atom]] {
  lazy val ruleLength: Int = body.length + 1

  override def hashCode(): Int = {
    val support = measures(Measure.Support).asInstanceOf[Measure.Support].value
    val headSize = measures(Measure.HeadSize).asInstanceOf[Measure.HeadSize].value
    body.size * headSize + support
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case rule: DanglingRule => Rule.checkBodyEquality(body, rule.body.toSet)
    case _ => false
  }
}
