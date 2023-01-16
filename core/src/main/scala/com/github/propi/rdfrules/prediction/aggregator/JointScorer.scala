package com.github.propi.rdfrules.prediction.aggregator

import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.ScoreFactory
import com.github.propi.rdfrules.rule.{DefaultConfidence, Rule}

import scala.collection.mutable

class JointScorer private(implicit defaultConfidence: DefaultConfidence) extends ScoreFactory {

  def newBuilder: mutable.Builder[Rule.FinalRule, Double] = {
    var res = 1.0
    new mutable.Builder[Rule.FinalRule, Double] {
      def clear(): Unit = res = 1.0

      def result(): Double = 1 - res

      def addOne(elem: Rule.FinalRule): this.type = {
        res = res * (1 - defaultConfidence.confidence(elem.measures))
        this
      }
    }
  }

}

object JointScorer {
  def apply()(implicit defaultConfidence: DefaultConfidence): ScoreFactory = new JointScorer
}