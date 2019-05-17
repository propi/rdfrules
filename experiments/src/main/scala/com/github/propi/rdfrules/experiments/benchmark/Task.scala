package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.utils.HowLong

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait Task[I1, I2, O1, O2] {

  self: TaskPreProcessor[I1, I2] with TaskPostProcessor[O1, O2] =>

  val name: String

  protected def taskBody(input: I2): O1

  final def execute(input: I1): (HowLong.Stats, O2) = {
    val preProcessedInput = preProcess(input)
    val output = postProcess(HowLong.howLong(name, memUsage = true, forceShow = true) {
      taskBody(preProcessedInput)
    })
    HowLong.get(name).get -> output
  }

}

object Task {

  object NoInput

}