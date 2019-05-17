package com.github.propi.rdfrules.experiments.benchmark

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

}