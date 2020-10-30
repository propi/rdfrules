package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.algorithm.consumer.TopKRuleConsumer
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class TopKRdfRules[T](val name: String,
                      val topK: Int,
                      override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                      override val allowConstants: Boolean = DefaultMiningSettings.allowConstants,
                      countConfidences: Boolean = false,
                      override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads)
                     (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

  override val ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(TopKRuleConsumer(topK))

  override protected def countOtherMetrics(ruleset: Ruleset): Ruleset = if (countConfidences) {
    super.countOtherMetrics(ruleset)
  } else {
    ruleset
  }

}