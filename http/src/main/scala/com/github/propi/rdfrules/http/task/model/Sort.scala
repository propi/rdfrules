package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sort(measures: Seq[(Option[TypedKeyMap.Key[Measure]], Boolean)]) extends Task[Model, Model] {
  val companion: TaskDefinition = Sort

  private def revNum(x: Boolean) = if (x) -1 else 1

  def execute(input: Model): Model = {
    val measuresConverters: Seq[ResolvedRule => Measure] = measures.map(x => x._1.collect {
      case Measure.BodySize => rule: ResolvedRule => Measure.BodySize(rule.measures.get(Measure.BodySize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Confidence => rule: ResolvedRule => Measure.Confidence(rule.measures.get(Measure.Confidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadConfidence => rule: ResolvedRule => Measure.HeadConfidence(rule.measures.get(Measure.HeadConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadCoverage => rule: ResolvedRule => Measure.HeadCoverage(rule.measures.get(Measure.HeadCoverage).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.HeadSize => rule: ResolvedRule => Measure.HeadSize(rule.measures.get(Measure.HeadSize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Lift => rule: ResolvedRule => Measure.Lift(rule.measures.get(Measure.Lift).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.PcaBodySize => rule: ResolvedRule => Measure.PcaBodySize(rule.measures.get(Measure.PcaBodySize).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.PcaConfidence => rule: ResolvedRule => Measure.PcaConfidence(rule.measures.get(Measure.PcaConfidence).map(_.value).getOrElse(0.0) * revNum(x._2))
      case Measure.Support => rule: ResolvedRule => Measure.Support(rule.measures.get(Measure.Support).map(_.value).getOrElse(0) * revNum(x._2))
      case Measure.Cluster => rule: ResolvedRule => Measure.Cluster(rule.measures.get(Measure.Cluster).map(_.number).getOrElse(0) * revNum(x._2))
    }.getOrElse((rule: ResolvedRule) => Measure.Cluster(rule.ruleLength * revNum(x._2))))
    input.sortBy[Iterable[Measure]] { rule =>
      measuresConverters.map(_ (rule))
    }
  }
}

object Sort extends TaskDefinition {
  val name: String = "Sort"
}