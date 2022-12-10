package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.experiments.benchmark.Metric.Simple
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.ResolvedRule
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
    def doubleValue: Double = value.toNanos.toDouble

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
        s"${round(value.toDouble, 4)} B"
      }
    }

    def doubleValue: Double = value.toDouble

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

  case class ClustersSimilarities(name: String, matrix: IndexedSeq[IndexedSeq[Double]]) extends Complex {
    private lazy val dunnIndex = {
      val (minInter, maxIntra) = (for {
        (row, c1) <- matrix.iterator.zipWithIndex
        (value, c2) <- row.iterator.zipWithIndex
      } yield {
        (if (c1 == c2) 1.0 else 1 - value) -> (if (c1 == c2) 1 - value else 0.0)
      }).reduce((x, y) => math.min(x._1, y._1) -> math.max(x._2, y._2))
      minInter / maxIntra
    }

    private lazy val rangeIndex = {
      if (matrix.length > 1) {
        matrix.indices.iterator.map { i =>
          val interMax = matrix.indices.iterator.filter(_ != i).map(matrix(i)(_)).max
          matrix(i)(i) - interMax
        }.sum / matrix.length
        /*var interTotal = 0
        val interSum = matrix.indices.combinations(2).map { indices =>
          val i = indices(0)
          val j = indices(1)
          interTotal += 1
          matrix(i)(j)
        }.sum
        val intraSum = matrix.indices.map(i => matrix(i)(i)).sum
        val interAvg = interSum / interTotal
        val intraAvg = intraSum / matrix.length
        intraAvg - interAvg*/
      } else {
        matrix.head.head
      }
    }

    private lazy val avgIntra = matrix.indices.iterator.map { i =>
      matrix(i)(i)
    }.sum / matrix.length

    private lazy val avgInter = matrix.indices.iterator.map { i =>
      matrix.indices.iterator.filter(_ != i).map(matrix(i)(_)).sum
    }.sum / (matrix.length * matrix.length - matrix.length)

    def doubleValue: Double = rangeIndex

    def getSimple: Simple = Number(name, doubleValue)

    override def toString: String = s"score: $doubleValue, dunnIndex: $dunnIndex, avgInter: $avgInter, avgIntra: $avgIntra matrix: \n" + matrix.iterator.map(_.mkString(", ")).mkString("\n")
  }

  case class Stats(avg: Simple, stdDev: Simple, min: Simple, max: Simple) extends Complex {
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

  implicit def indexToMetrics(index: Index): Seq[Metric] = {
    val thi = index.tripleMap
    val mapper = index.tripleItemMap
    val totalDisc = thi.predicates.iterator.map(mapper.getTripleItem).collect {
      case TripleItem.LongUri(uri) => uri
      case x: TripleItem.PrefixedUri => x.toLongUri.uri
    }.count(_.contains("_discretized_level_"))
    val numPredicates = thi.predicates.valuesIterator.count(_.objects.iterator.exists(mapper.getTripleItem(_).isInstanceOf[TripleItem.Number[_]]))
    val numIntervals = thi.predicates.iterator
      .filter(x => mapper.getTripleItem(x).asInstanceOf[TripleItem.Uri].uri.contains("_discretized_level_"))
      .flatMap { discPred =>
        thi.predicates(discPred).objects.iterator
          .map(mapper.getTripleItem)
          .collect {
            case x: TripleItem.Interval => x
          }
      }.size
    List(Number("predicates", thi.predicates.size), Number("numPredicates", numPredicates), Number("discretized_*", totalDisc), Number("triples", thi.size(false)), Number("intervals", numIntervals))
  }

  val simpleStringifier: Stringifier[Metric] = (v: Metric) => v.getSimple.prettyValue

  val signSimpleStringifier: Stringifier[Metric] = (v: Metric) => {
    val signChar = if (v.doubleValue >= 0) "+" else ""
    signChar + v.getSimple.prettyValue
  }

  val basicStringifier: Stringifier[Metric] = {
    case x: Simple => s"${x.name}: ${x.prettyValue}"
    case x: Complex => x match {
      case x: ClustersSimilarities => x.toString
      case Stats(avg, stdDev, min, max) => s"${x.name}: ${avg.prettyValue} (stdDev: ${stdDev.prettyValue}, min: ${min.prettyValue}, max: ${max.prettyValue})"
      case Comparison(metric, absDiff, relDiff) => Stringifier(metric)(basicStringifier) + ", " + Stringifier(absDiff)(signSimpleStringifier) + s" (${Stringifier[Metric](Number("", relDiff * 100))(signSimpleStringifier)}%)"
    }
  }

}