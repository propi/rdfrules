package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule {
  val body: Iterable[Atom]
  val head: Atom
  val measures: Measure.Measures
  val ruleLength: Int
}

case class ClosedRule(body: List[Atom], head: Atom, measures: Measure.Measures) extends Rule {
  lazy val ruleLength: Int = body.length + 1
}

case class DanglingRule(body: List[Atom], head: Atom, measures: Measure.Measures) extends Rule {
  lazy val ruleLength: Int = body.length + 1
}
