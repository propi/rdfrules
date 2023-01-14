package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.rule.Rule.FinalRule

sealed trait PredictedTriple {
  def triple: IntTriple

  def rules: Iterable[FinalRule]

  def predictedResult: PredictedResult
}

object PredictedTriple {

  sealed trait Single extends PredictedTriple {
    def rule: FinalRule
  }

  sealed trait Grouped extends PredictedTriple {
    def score: Double
  }

  private case class Basic(triple: IntTriple, rule: FinalRule)(val predictedResult: PredictedResult) extends Single {
    def rules: Seq[FinalRule] = List(rule)
  }

  private case class BasicGrouped(triple: IntTriple)(val rules: Iterable[FinalRule], val predictedResult: PredictedResult, val score: Double) extends Grouped

  def apply(triple: IntTriple, predictedResult: PredictedResult, rule: FinalRule): Single = Basic(triple, rule)(predictedResult)

  def apply(triple: IntTriple, predictedResult: PredictedResult, rules: Iterable[FinalRule], score: Double): Grouped = BasicGrouped(triple)(rules, predictedResult, score)

  implicit class PimpedPredictedTriple(val predictedTriple: PredictedTriple) extends AnyVal {
    def score: Double = predictedTriple match {
      case x: Grouped => x.score
      case _ => 0.0
    }

    def predictionTasks: (PredictionTask, PredictionTask) = PredictionTask(predictedTriple.triple, TriplePosition.Object) -> PredictionTask(predictedTriple.triple, TriplePosition.Subject)

    def toSinglePredictedTriples: Iterator[Single] = predictedTriple match {
      case x: Single => Iterator(x)
      case x => x.rules.iterator.map(rule => apply(x.triple, x.predictedResult, rule))
    }
  }

}