package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
trait DefaultMiningSettings {

  val allowConstants: Boolean = false
  val maxRuleLength: Int = 3
  val minPcaConfidence: Double = 0.1
  val minConfidence: Double = 0.1
  val minHeadCoverage: Double = 0.01
  val numberOfThreads: Int = Runtime.getRuntime.availableProcessors()
  val ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer())
  val experiment: Boolean = false
  val withoutDuplicatePredicates: Boolean = false
  val injectiveMapping: Boolean = true
  val constantsEverywhere: Boolean = false

}

object DefaultMiningSettings extends DefaultMiningSettings