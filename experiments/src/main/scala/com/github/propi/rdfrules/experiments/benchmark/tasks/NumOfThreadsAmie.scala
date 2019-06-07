package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{AmieRulesMiningTask, DefaultMiningSettings}

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class NumOfThreadsAmie(val name: String,
                       override val numberOfThreads: Int,
                       override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                       override val allowConstants: Boolean = DefaultMiningSettings.allowConstants) extends AmieRulesMiningTask