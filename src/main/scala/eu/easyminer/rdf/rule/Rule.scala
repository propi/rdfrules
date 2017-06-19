package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule[T <: Iterable[Atom] {def reverse : T}] {
  val body: T
  val head: Atom
  val measures: Measure.Measures
  val ruleLength: Int
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

}

case class ClosedRule(body: List[Atom], head: Atom, measures: Measure.Measures, variables: List[Atom.Variable]) extends Rule[List[Atom]] {
  lazy val ruleLength: Int = body.length + 1
}

case class DanglingRule(body: List[Atom], head: Atom, measures: Measure.Measures, variables: Rule.DanglingVariables) extends Rule[List[Atom]] {
  lazy val ruleLength: Int = body.length + 1
}
