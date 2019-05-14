package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.utils.HowLong

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait Task[I, O1, O2] {

  val name: String

  protected def taskBody(input: I): O1

  protected def postProcess(result: O1): O2

  final def execute(input: I): (HowLong.Stats, O2) = {
    val output = postProcess(HowLong.howLong(name, memUsage = true, forceShow = true) {
      taskBody(input)
    })
    HowLong.get(name).get -> output
  }

}
