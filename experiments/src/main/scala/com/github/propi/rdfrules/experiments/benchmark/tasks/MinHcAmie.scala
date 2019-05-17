package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.AmieRulesMiningTask

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class MinHcAmie[T](val name: String, override val minHeadCoverage: Double, override val allowConstants: Boolean = false) extends AmieRulesMiningTask