package com.github.propi.rdfrules.stringifier

import com.github.propi.rdfrules.data.{Quad, Triple}
import com.github.propi.rdfrules.index.TripleItemHashIndex
import com.github.propi.rdfrules.rule.{Measure, Rule}
import com.github.propi.rdfrules.ruleset.ResolvedRule

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
object CommonStringifiers {

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => v.subject + "  " + v.predicate + "  " + v.`object`

  implicit val quadStringifier: Stringifier[Quad] = (v: Quad) => Stringifier(v.triple) + " " + v.graph

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
    case Measure.Cluster(v) => s"cluster: $v"
  }

  implicit val itemStringifier: Stringifier[ResolvedRule.Atom.Item] = {
    case ResolvedRule.Atom.Item.Variable(x) => x.value
    case ResolvedRule.Atom.Item.Constant(x) => x.toString
  }

  implicit val atomStringifier: Stringifier[ResolvedRule.Atom] = {
    case ResolvedRule.Atom.Basic(s, p, o) => s"(${Stringifier(s)} ${p.toString} ${Stringifier(o)})"
    case v@ResolvedRule.Atom.GraphBased(s, p, o) =>
      def bracketGraphs(strGraphs: String): String = if (v.graphs.size == 1) strGraphs else s"[$strGraphs]"

      s"(${Stringifier(s)} ${p.toString} ${Stringifier(o)} ${bracketGraphs(v.graphs.iterator.map(_.toString).mkString(", "))})"
  }

  implicit val resolvedRuleStringifier: Stringifier[ResolvedRule] = (v: ResolvedRule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.iterator.toList.sortBy(_.companion).iterator.map(x => Stringifier(x)).mkString(", ")

  implicit def ruleStringifier(implicit resolvedRuleStringifier: Stringifier[ResolvedRule], mapper: TripleItemHashIndex): Stringifier[Rule] = (v: Rule) => resolvedRuleStringifier.toStringValue(v)

  implicit def ruleSimpleStringifier(implicit ruleStringifier: Stringifier[Rule]): Stringifier[Rule.Simple] = (v: Rule.Simple) => ruleStringifier.toStringValue(v)

}