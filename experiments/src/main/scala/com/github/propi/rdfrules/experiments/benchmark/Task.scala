package com.github.propi.rdfrules.experiments.benchmark

import scala.concurrent.duration.Duration

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait Task[I1, I2, O1, O2] {

  self: TaskPreProcessor[I1, I2] with TaskPostProcessor[O1, O2] =>

  val name: String

  protected def taskBody(input: I2): O1

  private def getCurrentMemory = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

  final def execute(input: I1): (Seq[Metric], O2) = {
    val preProcessedInput = preProcess(input)
    val beforeUsedMem = {
      System.gc()
      getCurrentMemory
    }
    val time = System.nanoTime()
    val x = taskBody(preProcessedInput)
    val runningTime = Duration.fromNanos(System.nanoTime() - time)
    val afterUsedMem = {
      System.gc()
      getCurrentMemory
    }
    List(Metric.Duration("time", runningTime), Metric.Memory("memory", afterUsedMem - beforeUsedMem)) -> postProcess(x)
  }

}