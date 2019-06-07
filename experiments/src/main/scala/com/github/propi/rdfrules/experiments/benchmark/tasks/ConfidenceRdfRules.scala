package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RulesetTask, TaskPostProcessor}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 18. 5. 2019.
  */
class ConfidenceRdfRules[T](val name: String,
                            override val minConfidence: Double = 0.0,
                            override val minPcaConfidence: Double = 0.0,
                            countLift: Boolean = false,
                            topK: Int = 0,
                            override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads
                           )(implicit val debugger: Debugger) extends RulesetTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

  protected def preProcess(input: Ruleset): Ruleset = input.setParallelism(numberOfThreads)

  protected def taskBody(input: Ruleset): Ruleset = Function.chain[Ruleset](List(
    x => if (minConfidence > 0.0) x.computeConfidence(minConfidence, topK) else x,
    x => if (minPcaConfidence > 0.0) x.computePcaConfidence(minPcaConfidence, topK) else x,
    x => if (countLift && minConfidence > 0.0) x.computeLift(minConfidence) else x
  ))(input).cache

}