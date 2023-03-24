package com.github.propi.rdfrules.prediction.aggregator

import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.ScoreFactory
import com.github.propi.rdfrules.rule.{DefaultConfidence, Rule}

import scala.collection.mutable

class MaximumScorer private(implicit defaultConfidence: DefaultConfidence) extends ScoreFactory {
  def newBuilder: mutable.Builder[Rule.FinalRule, Double] = {
    var maxConfidence: Double = 0.0
    new mutable.Builder[Rule.FinalRule, Double] {
      def clear(): Unit = maxConfidence = 0.0

      def result(): Double = maxConfidence

      def addOne(elem: Rule.FinalRule): this.type = {
        maxConfidence = math.max(maxConfidence, defaultConfidence.confidence(elem.measures))
        this
      }
    }
  }
}

object MaximumScorer {
  def apply()(implicit defaultConfidence: DefaultConfidence = DefaultConfidence()): ScoreFactory = new MaximumScorer
}