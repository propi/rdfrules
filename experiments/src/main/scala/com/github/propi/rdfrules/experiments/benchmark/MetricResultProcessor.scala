package com.github.propi.rdfrules.experiments.benchmark

import java.io.PrintStream

import com.github.propi.rdfrules.utils.Stringifier

/**
  * Created by Vaclav Zeman on 19. 5. 2019.
  */
trait MetricResultProcessor[T] {

  def processMetrics(metrics: (String, Seq[Metric])): T

}

object MetricResultProcessor {

  class BasicPrinter private(out: PrintStream) extends MetricResultProcessor[Unit] {
    def processMetrics(metrics: (String, Seq[Metric])): Unit = {
      out.println(s"******* TASK: ${metrics._1} *******")
      for (metric <- metrics._2.sortBy(_.name)) {
        out.println(Stringifier(metric)(Metric.basicStringifier))
      }
      println(s"***********************************")
    }
  }

  object BasicPrinter {
    def apply()(implicit out: PrintStream): BasicPrinter = new BasicPrinter(out)
  }

}