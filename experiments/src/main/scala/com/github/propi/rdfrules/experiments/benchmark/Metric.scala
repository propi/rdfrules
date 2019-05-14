package com.github.propi.rdfrules.experiments.benchmark

import scala.concurrent.duration

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait Metric {
  val name: String
}

object Metric {

  case class Duration(name: String, value: duration.Duration) extends Metric

  case class Number[T](name: String, value: T)(implicit n: Numeric[T]) extends Metric

}