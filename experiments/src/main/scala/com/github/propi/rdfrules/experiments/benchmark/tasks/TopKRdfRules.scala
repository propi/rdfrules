package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class TopKRdfRules[T](val name: String,
                      val topK: Int,
                      override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                      override val allowConstants: Boolean = DefaultMiningSettings.allowConstants,
                      countConfidences: Boolean = false)
                     (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

  override protected def miningTask(rulesMining: RulesMining): RulesMining = rulesMining.addThreshold(Threshold.TopK(topK))

  override protected def countOtherMetrics(ruleset: Ruleset): Ruleset = if (countConfidences) {
    super.countOtherMetrics(ruleset)
  } else {
    ruleset
  }

}