package com.github.propi.rdfrules.experiments.benchmark.metrics

import com.github.propi.rdfrules.experiments.benchmark.Metric
import com.github.propi.rdfrules.ruleset.Ruleset

class RulesetMetric(name: String, val ruleset: Ruleset) extends Metric.UniversalMetric(name) {
  def doubleValue: Double = ruleset.size

  override def toString: String = s"$name: $doubleValue"
}
