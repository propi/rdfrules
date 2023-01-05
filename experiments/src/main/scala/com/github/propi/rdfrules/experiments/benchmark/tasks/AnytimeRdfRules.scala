package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class AnytimeRdfRules[T](val name: String,
                         timeout: Threshold.LocalTimeout,
                         override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                         override val allowConstants: Option[ConstantsPosition] = DefaultMiningSettings.allowConstants,
                         override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads)
                        (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {
  self: TaskPostProcessor[Ruleset, T] =>

  override val minPcaConfidence: Double = 0.0
  override val minConfidence: Double = 0.0
  override val localTimeout: Option[Threshold.LocalTimeout] = Some(timeout)
}