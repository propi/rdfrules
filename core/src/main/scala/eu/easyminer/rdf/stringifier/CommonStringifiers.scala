package eu.easyminer.rdf.stringifier

import eu.easyminer.rdf.data.Triple
import eu.easyminer.rdf.index.TripleItemHashIndex
import eu.easyminer.rdf.rule.Atom.Item
import eu.easyminer.rdf.rule.{Atom, Measure, Rule}

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
object CommonStringifiers {

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => v.subject + "  " + v.predicate + "  " + v.`object`

  implicit def itemStringifier(implicit mapper: TripleItemHashIndex): Stringifier[Item] = {
    case x: Atom.Variable => x.value
    case Atom.Constant(x) => mapper.getTripleItem(x).toString
  }

  implicit def atomStringifier(implicit mapper: TripleItemHashIndex): Stringifier[Atom] = (v: Atom) => s"(${Stringifier(v.subject)} ${mapper.getTripleItem(v.predicate).toString} ${Stringifier(v.`object`)})"

  implicit def ruleStringifier(implicit mapper: TripleItemHashIndex): Stringifier[Rule] = (v: Rule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.iterator.toList.sortBy(_.companion).iterator.map(x => Stringifier(x)).mkString(", ")

  implicit val measureStringifier: Stringifier[Measure] = {
    case Measure.Support(v) => s"support: $v"
    case Measure.HeadCoverage(v) => s"headCoverage: $v"
    case Measure.Confidence(v) => s"confidence: $v"
    case Measure.Lift(v) => s"lift: $v"
    case Measure.PcaConfidence(v) => s"pcaConfidence: $v"
    case Measure.PcaLift(v) => s"pcaLift: $v"
    case Measure.HeadConfidence(v) => s"headConfidence: $v"
    case Measure.HeadSize(v) => s"headSize: $v"
    case Measure.BodySize(v) => s"bodySize: $v"
    case Measure.PcaBodySize(v) => s"pcaBodySize: $v"
  }

}
