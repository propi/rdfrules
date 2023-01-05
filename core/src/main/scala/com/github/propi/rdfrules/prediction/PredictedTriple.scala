package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.rule.Rule.FinalRule

sealed trait PredictedTriple {
  def triple: IntTriple

  def rules: Iterable[FinalRule]

  def predictedResult: PredictedResult

  def :+(rule: FinalRule): PredictedTriple

  def :++(rules: Iterable[FinalRule]): PredictedTriple
}

object PredictedTriple {

  sealed trait Single extends PredictedTriple {
    def rule: FinalRule
  }

  sealed trait Scored extends PredictedTriple {
    def score: Double

    def :+(rule: FinalRule): Scored

    def :++(rules: Iterable[FinalRule]): Scored
  }

  private case class Basic(triple: IntTriple, rule: FinalRule)(val predictedResult: PredictedResult) extends Single {
    def rules: Seq[FinalRule] = List(rule)

    def :+(rule: FinalRule): PredictedTriple = BasicGrouped(triple)(Vector(rule, this.rule), predictedResult)

    def :++(rules: Iterable[FinalRule]): PredictedTriple = BasicGrouped(triple)(Vector.from(rules.iterator ++ Iterator(rule)), predictedResult)
  }

  private case class ScoredBasicGrouped(triple: IntTriple)(val rules: Vector[FinalRule], val predictedResult: PredictedResult, val score: Double) extends Scored {
    def :+(rule: FinalRule): Scored = copy()(this.rules :+ rule, predictedResult, score)

    def :++(rules: Iterable[FinalRule]): Scored = copy()(this.rules :++ rules, predictedResult, score)
  }

  private case class BasicGrouped(triple: IntTriple)(val rules: Vector[FinalRule], val predictedResult: PredictedResult) extends PredictedTriple {
    def :+(rule: FinalRule): PredictedTriple = copy()(this.rules :+ rule, predictedResult)

    def :++(rules: Iterable[FinalRule]): PredictedTriple = copy()(this.rules :++ rules, predictedResult)
  }

  def apply(triple: IntTriple, predictedResult: PredictedResult, rule: FinalRule): Single = Basic(triple, rule)(predictedResult)

  implicit class PimpedPredictedTriple(val predictedTriple: PredictedTriple) extends AnyVal {
    def withScore(score: Double): Scored = ScoredBasicGrouped(predictedTriple.triple)(predictedTriple.rules.toVector, predictedTriple.predictedResult, score)

    def toSinglePredictedTriples: Iterator[Single] = predictedTriple match {
      case x: Single => Iterator(x)
      case x => x.rules.iterator.map(rule => apply(x.triple, x.predictedResult, rule))
    }
  }

}