package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.TypedKeyMap

import scala.math.Ordering.Implicits.seqOrdering

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sort(measures: Seq[(Option[TypedKeyMap.Key[Measure]], Boolean)]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Sort

  private def revNum(x: Boolean) = if (x) -1 else 1

  def execute(input: Ruleset): Ruleset = {
    if (measures.isEmpty) {
      input.sorted
    } else {
      val measuresConverters: Seq[FinalRule => Measure] = measures.map(x => x._1.collect {
        case Measure.BodySize => rule: FinalRule => Measure.BodySize(rule.measures.get(Measure.BodySize).map(_.value).getOrElse(0) * revNum(x._2))
        case Measure.Confidence => rule: FinalRule => Measure.Confidence(rule.measures.get(Measure.Confidence).map(_.value).getOrElse(0.0) * revNum(x._2))
        case Measure.HeadConfidence => rule: FinalRule => Measure.HeadConfidence(rule.measures.get(Measure.HeadConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
        case Measure.HeadCoverage => rule: FinalRule => Measure.HeadCoverage(rule.measures.get(Measure.HeadCoverage).map(_.value).getOrElse(0.0) * revNum(x._2))
        case Measure.HeadSize => rule: FinalRule => Measure.HeadSize(rule.measures.get(Measure.HeadSize).map(_.value).getOrElse(0) * revNum(x._2))
        case Measure.Lift => rule: FinalRule => Measure.Lift(rule.measures.get(Measure.Lift).map(_.value).getOrElse(0.0) * revNum(x._2))
        case Measure.PcaBodySize => rule: FinalRule => Measure.PcaBodySize(rule.measures.get(Measure.PcaBodySize).map(_.value).getOrElse(0) * revNum(x._2))
        case Measure.PcaConfidence => rule: FinalRule => Measure.PcaConfidence(rule.measures.get(Measure.PcaConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
        case Measure.Support => rule: FinalRule => Measure.Support(rule.measures.get(Measure.Support).map(_.value).getOrElse(0) * revNum(x._2))
        case Measure.Cluster => rule: FinalRule => Measure.Cluster(rule.measures.get(Measure.Cluster).map(_.number).getOrElse(0) * revNum(x._2))
      }.getOrElse((rule: FinalRule) => Measure.Cluster(rule.ruleLength * revNum(x._2))))
      input.sortBy { rule =>
        measuresConverters.map(_ (rule))
      }
    }
  }
}

object Sort extends TaskDefinition {
  val name: String = "SortRuleset"
}