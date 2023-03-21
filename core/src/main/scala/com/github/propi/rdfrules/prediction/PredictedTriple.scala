package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.rule.DefaultConfidence
import com.github.propi.rdfrules.rule.Rule.FinalRule

sealed trait PredictedTriple {
  def triple: IntTriple

  def rules: Iterable[FinalRule]

  def predictedResult: PredictedResult

  def score: Double

  final def predictionTasks: (PredictionTask, PredictionTask) = PredictionTask(triple, TriplePosition.Object) -> PredictionTask(triple, TriplePosition.Subject)
}

object PredictedTriple {

  sealed trait Single extends PredictedTriple {
    def rule: FinalRule
  }

  sealed trait Grouped extends PredictedTriple

  private case class Basic(triple: IntTriple, rule: FinalRule)(val predictedResult: PredictedResult) extends Single {
    def rules: Seq[FinalRule] = List(rule)

    def score: Double = 0.0
  }

  private case class BasicGrouped(triple: IntTriple)(val rules: Iterable[FinalRule], val predictedResult: PredictedResult, val score: Double) extends Grouped

  def apply(triple: IntTriple, predictedResult: PredictedResult, rule: FinalRule): Single = Basic(triple, rule)(predictedResult)

  def apply(triple: IntTriple, predictedResult: PredictedResult, rules: Iterable[FinalRule], score: Double): Grouped = BasicGrouped(triple)(rules, predictedResult, score)

  implicit class PimpedPredictedTriple(val predictedTriple: PredictedTriple) extends AnyVal {
    def toSinglePredictedTriples: Iterator[Single] = predictedTriple match {
      case x: Single => Iterator(x)
      case x => x.rules.iterator.map(rule => apply(x.triple, x.predictedResult, rule))
    }
  }

  private def compareIts(it1: Iterator[Double], it2: Iterator[Double]) = {
    it1.zipAll(it2, 0.0, 0.0).map(x => if (x._1 < x._2) 1 else if (x._1 > x._2) -1 else 0).find(_ != 0).getOrElse(0)
  }

  implicit def predictedTripleOrdering(implicit defaultConfidence: DefaultConfidence): Ordering[PredictedTriple] = (x: PredictedTriple, y: PredictedTriple) => {
    if (x.score < y.score) 1 else if (x.score > y.score) -1 else {
      val confs1 = x.rules.iterator.flatMap(rule => defaultConfidence.confidenceOpt(rule.measures))
      val confs2 = y.rules.iterator.flatMap(rule => defaultConfidence.confidenceOpt(rule.measures))
      val res = compareIts(confs1, confs2)
      if (res == 0) {
        val sup1 = x.rules.iterator.map(rule => rule.headCoverage)
        val sup2 = y.rules.iterator.map(rule => rule.headCoverage)
        compareIts(sup1, sup2)
      } else {
        res
      }
    }
  }

}