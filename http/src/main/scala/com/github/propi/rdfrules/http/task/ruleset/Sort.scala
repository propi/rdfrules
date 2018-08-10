package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sort(measures: Seq[(Option[TypedKeyMap.Key[Measure]], Boolean)]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Sort

  private def revNum(x: Boolean) = if (x) -1 else 1

  def execute(input: Ruleset): Ruleset = {
    input.sortBy[Iterable[Measure]] { rule =>
      measures.map {
        case (Some(measure), reverse) => measure match {
          case Measure.BodySize(x) => Measure.BodySize(x * revNum(reverse))
          case Measure.Confidence(x) => Measure.Confidence(x * revNum(reverse))
          case Measure.HeadConfidence(x) => Measure.HeadConfidence(x * revNum(reverse))
          case Measure.HeadCoverage(x) => Measure.HeadCoverage(x * revNum(reverse))
          case Measure.HeadSize(x) => Measure.HeadSize(x * revNum(reverse))
          case Measure.Lift(x) => Measure.Lift(x * revNum(reverse))
          case Measure.PcaBodySize(x) => Measure.PcaBodySize(x * revNum(reverse))
          case Measure.PcaConfidence(x) => Measure.PcaConfidence(x * revNum(reverse))
          case Measure.Support(x) => Measure.Support(x * revNum(reverse))
          case Measure.Cluster(x) => Measure.Cluster(x * revNum(reverse))
        }
        case (None, reverse) => Measure.Cluster(rule.ruleLength * revNum(reverse))
      }
    }
  }
}

object Sort extends TaskDefinition {
  val name: String = "Sort"
}