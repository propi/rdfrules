package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.{Measure, Rule}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sort(measures: Seq[(Option[TypedKeyMap.Key[Measure]], Boolean)]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Sort

  private def revNum(x: Boolean) = if (x) -1 else 1

  def execute(input: Ruleset): Ruleset = {
    val measuresConverters: Seq[Rule.Simple => Measure] = measures.map(x => x._1.collect {
      case Measure.BodySize => rule: Rule.Simple => Measure.BodySize(rule.measures.get(Measure.BodySize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Confidence => rule: Rule.Simple => Measure.Confidence(rule.measures.get(Measure.Confidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadConfidence => rule: Rule.Simple => Measure.HeadConfidence(rule.measures.get(Measure.HeadConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadCoverage => rule: Rule.Simple => Measure.HeadCoverage(rule.measures.get(Measure.HeadCoverage).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadSize => rule: Rule.Simple => Measure.HeadSize(rule.measures.get(Measure.HeadSize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Lift => rule: Rule.Simple => Measure.Lift(rule.measures.get(Measure.Lift).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.PcaBodySize => rule: Rule.Simple => Measure.PcaBodySize(rule.measures.get(Measure.PcaBodySize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.PcaConfidence => rule: Rule.Simple => Measure.PcaConfidence(rule.measures.get(Measure.PcaConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.Support => rule: Rule.Simple => Measure.Support(rule.measures.get(Measure.Support).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Cluster => rule: Rule.Simple => Measure.Cluster(rule.measures.get(Measure.Cluster).map(_.number).getOrElse(0) * revNum(x._2))
    }.getOrElse((rule: Rule.Simple) => Measure.Cluster(rule.ruleLength * revNum(x._2))))
    input.sortBy[Iterable[Measure]] { rule =>
      measuresConverters.map(_ (rule))
    }
  }
}

object Sort extends TaskDefinition {
  val name: String = "Sort"
}