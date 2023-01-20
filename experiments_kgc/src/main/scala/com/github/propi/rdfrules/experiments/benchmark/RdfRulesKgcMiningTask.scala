package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.consumer.TopKRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class RdfRulesKgcMiningTask(val name: String,
                            exportPath: String,
                            cores: Int,
                            _maxRuleLength: Int,
                            anytime: Boolean,
                            constants: ConstantsAtPosition.ConstantsPosition
                           )(implicit val debugger: Debugger) extends RdfRulesMiningTask[Ruleset] with TaskPostProcessor[Ruleset, Ruleset] {
  override val allowConstants: Option[ConstantsAtPosition.ConstantsPosition] = Some(constants)
  override val maxRuleLength: Int = _maxRuleLength
  override val minQpcaConfidence: Double = 0.0
  override val minPcaConfidence: Double = 0.0
  override val minConfidence: Double = 0.0
  override val numberOfThreads: Int = cores
  override val localTimeout: Option[Threshold.LocalTimeout] = if (anytime) Some(Threshold.LocalTimeout(0.01, true)) else None
  override val ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(TopKRuleConsumer(2000000, true))

  override protected def miningTask(rulesMining: RulesMining): RulesMining = rulesMining
    .addThreshold(Threshold.MinSupport(5))
    .addThreshold(Threshold.MinHeadSize(1))

  protected def postProcess(result: Ruleset): Ruleset = {
    result.`export`(exportPath)
    result
  }
}
