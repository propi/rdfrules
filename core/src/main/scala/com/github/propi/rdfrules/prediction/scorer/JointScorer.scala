package com.github.propi.rdfrules.prediction.scorer

import com.github.propi.rdfrules.prediction.{PredictedTriple, PredictionScorer}
import com.github.propi.rdfrules.rule.{Measure, Rule}

object JointScorer extends PredictionScorer {
  private def confidence(rule: Rule): Double = rule.measures.get[Measure.QpcaConfidence]
    .orElse(rule.measures.get[Measure.PcaConfidence])
    .orElse(rule.measures.get[Measure.CwaConfidence])
    .map(_.value)
    .getOrElse(0.0)

  def score(predictedTriple: PredictedTriple): Double = 1 - predictedTriple.rules.iterator.map(1 - confidence(_)).product
}