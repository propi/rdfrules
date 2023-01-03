package com.github.propi.rdfrules.experiments.benchmark.postprocessors

import com.github.propi.rdfrules.experiments.benchmark.{Metric, TaskPostProcessor}
import com.github.propi.rdfrules.experiments.benchmark.postprocessors.RulesDiffsPostprocessor.RulesDiffsStats
import com.github.propi.rdfrules.rule.{Rule, RuleContent}
import com.github.propi.rdfrules.ruleset.Ruleset

import scala.language.implicitConversions

trait RulesDiffsPostprocessor extends TaskPostProcessor[Ruleset, RulesDiffsStats] {
  protected val relevantRules: Map[RuleContent, Rule]

  protected def postProcess(result: Ruleset): RulesDiffsStats = result.rules.foldLeft(RulesDiffsStats(relevantRules.size, 0, 0, 0L, 0.0)) { (res, rule) =>
    relevantRules.get(RuleContent(rule.body, rule.head)) match {
      case Some(relevantRule) => res.addRetrievedRule(true, math.abs(rule.support - relevantRule.support).toLong, math.abs(rule.headCoverage - relevantRule.headCoverage))
      case None => res.addRetrievedRule(false, 0L, 0.0)
    }
  }
}

object RulesDiffsPostprocessor {

  case class RulesDiffsStats(relevantRulesSize: Int, retrievedRulesSize: Int, intersections: Int, supportDevs: Long, hcDevs: Double) {
    def precision: Double = if (retrievedRulesSize == 0) 0.0 else intersections.toDouble / retrievedRulesSize

    def recall: Double = if (relevantRulesSize == 0) 0.0 else intersections.toDouble / relevantRulesSize

    def fmeasure: Double = {
      val p = precision
      val r = recall
      val par = p + r
      if (par == 0.0) 0.0 else (2 * p * r) / par
    }

    def supportDev: Double = if (intersections == 0) 0.0 else supportDevs / intersections.toDouble

    def hcDev: Double = if (intersections == 0) 0.0 else hcDevs / intersections

    def addRetrievedRule(isIntersected: Boolean, supportDev: Long, hcDev: Double): RulesDiffsStats = RulesDiffsStats(
      relevantRulesSize,
      retrievedRulesSize + 1,
      if (isIntersected) intersections + 1 else intersections,
      supportDevs + supportDev,
      hcDevs + hcDev
    )
  }

  implicit def rulesDiffsStatsToMetrics(rulesDiffsStats: RulesDiffsStats): Seq[Metric] = List(
    Metric.Number("retrieved", rulesDiffsStats.retrievedRulesSize),
    Metric.Number("intersections", rulesDiffsStats.intersections),
    Metric.Number("precision", rulesDiffsStats.precision),
    Metric.Number("recall", rulesDiffsStats.recall),
    Metric.Number("fmeasure", rulesDiffsStats.fmeasure),
    Metric.Number("supportDev", rulesDiffsStats.supportDev),
    Metric.Number("hcDev", rulesDiffsStats.hcDev)
  )
}