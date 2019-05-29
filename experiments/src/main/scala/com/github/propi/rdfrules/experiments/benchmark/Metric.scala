package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.experiments.benchmark.Metric.Simple
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.BasicFunctions.round
import com.github.propi.rdfrules.utils.PrettyDuration._
import com.github.propi.rdfrules.utils.Stringifier

import scala.concurrent.duration
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
sealed trait Metric {
  val name: String

  def doubleValue: Double

  def getSimple: Simple
}

object Metric {

  sealed trait Simple extends Metric {
    def update(value: Double): Simple

    def +(x: Simple): Simple

    def -(x: Simple): Simple

    def /(x: Double): Simple = update(doubleValue / x)

    def getSimple: Simple = this

    def prettyValue: String
  }

  sealed trait Complex extends Metric

  case class Duration(name: String, value: duration.Duration) extends Simple {
    def doubleValue: Double = value.toNanos

    def update(value: Double): Duration = copy(value = duration.Duration.fromNanos(value))

    def +(x: Simple): Simple = x match {
      case y: Duration => update(doubleValue + y.doubleValue)
      case _ => x
    }

    def -(x: Simple): Simple = x match {
      case y: Duration => update(doubleValue - y.doubleValue)
      case _ => x
    }

    def prettyValue: String = value.pretty
  }

  case class Memory(name: String, value: Long) extends Simple {
    def prettyValue: String = {
      val absValue = math.abs(value)
      if (absValue >= 1000000000L) {
        s"${round(value / 1000000000.0, 4)} GB"
      } else if (absValue >= 1000000L) {
        s"${round(value / 1000000.0, 4)} MB"
      } else if (absValue >= 1000L) {
        s"${round(value / 1000.0, 4)} kB"
      } else {
        s"${round(value, 4)} B"
      }
    }

    def doubleValue: Double = value

    def update(value: Double): Simple = copy(value = value.toLong)

    def +(x: Simple): Simple = x match {
      case y: Memory => update(doubleValue + y.doubleValue)
      case _ => x
    }

    def -(x: Simple): Simple = x match {
      case y: Memory => update(doubleValue - y.doubleValue)
      case _ => x
    }
  }

  case class Number(name: String, value: Double) extends Simple {
    def prettyValue: String = round(value, 4).toString

    def doubleValue: Double = value

    def update(value: Double): Simple = copy(value = value)

    def +(x: Simple): Simple = x match {
      case y: Number => update(doubleValue + y.doubleValue)
      case _ => x
    }

    def -(x: Simple): Simple = x match {
      case y: Number => update(doubleValue - y.doubleValue)
      case _ => x
    }
  }

  case class Stats(avg: Simple, stdDev: Simple) extends Complex {
    val name: String = avg.name

    def doubleValue: Double = avg.doubleValue

    def getSimple: Simple = avg
  }

  case class Comparison(metric: Metric, absDiff: Metric, relDiff: Double) extends Complex {
    val name: String = metric.name

    def doubleValue: Double = metric.doubleValue

    def getSimple: Simple = metric match {
      case x: Simple => x
      case x: Complex => x.getSimple
    }

    private def sigChar(x: Double): String = if (x >= 0) "+" else ""

    override def toString: String = metric.toString + s" (absDiff: ${sigChar(absDiff.doubleValue)}$absDiff, relDiff: ${sigChar(relDiff)}${relDiff * 100}%)"
  }

  object Comparison {
    def apply(metric1: Metric, metric2: Metric): Comparison = new Comparison(
      metric1,
      metric1.getSimple - metric2.getSimple,
      math.signum(metric1.doubleValue - metric2.doubleValue) * (if (metric1.doubleValue >= metric2.doubleValue) {
        (metric1.doubleValue / metric2.doubleValue) - 1.0
      } else {
        1.0 - (metric1.doubleValue / metric2.doubleValue)
      })
    )
  }

  implicit def rulesToMetrics(rules: IndexedSeq[ResolvedRule]): Seq[Metric] = List(Number("rules", rules.length))

  val simpleStringifier: Stringifier[Metric] = (v: Metric) => v.getSimple.prettyValue

  val signSimpleStringifier: Stringifier[Metric] = (v: Metric) => {
    val signChar = if (v.doubleValue >= 0) "+" else ""
    signChar + v.getSimple.prettyValue
  }

  val basicStringifier: Stringifier[Metric] = {
    case x: Simple => s"${x.name}: ${x.prettyValue}"
    case x: Complex => x match {
      case Stats(avg, stdDev) => s"${x.name}: ${avg.prettyValue} (stdDev: ${stdDev.prettyValue})"
      case Comparison(metric, absDiff, relDiff) => Stringifier(metric)(basicStringifier) + ", " + Stringifier(absDiff)(signSimpleStringifier) + s" (${Stringifier[Metric](Number("", relDiff * 100))(signSimpleStringifier)}%)"
    }
  }

}