package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.AmieRulesMiningTask

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class NumOfThreadsAmie[T](val name: String,
                          override val numberOfThreads: Int,
                          override val minHeadCoverage: Double = super.minHeadCoverage,
                          override val allowConstants: Boolean = super.allowConstants) extends AmieRulesMiningTask