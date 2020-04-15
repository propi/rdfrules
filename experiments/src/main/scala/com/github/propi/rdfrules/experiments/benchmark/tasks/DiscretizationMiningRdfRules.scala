package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, DiscretizedRuleFilter, RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class DiscretizationMiningRdfRules[T](val name: String,
                                      filter: DiscretizedRuleFilter,
                                      override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                                      override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads)
                                     (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

  override val allowConstants: Boolean = true
  override val withConstantsAtTheObjectPosition: Boolean = true
  override val minPcaConfidence: Double = 0.0
  override val minConfidence: Double = 0.0

  override protected def miningTask(rulesMining: RulesMining): RulesMining = rulesMining.addConstraint(filter)

}