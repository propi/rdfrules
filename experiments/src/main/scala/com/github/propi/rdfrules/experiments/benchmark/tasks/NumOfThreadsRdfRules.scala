package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class NumOfThreadsRdfRules[T](val name: String,
                              override val numberOfThreads: Int,
                              override val minHeadCoverage: Double = super.minHeadCoverage,
                              override val allowConstants: Boolean = super.allowConstants)
                             (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

}