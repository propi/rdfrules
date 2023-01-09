package com.github.propi.rdfrules.prediction

trait PredictionScorer {
  def score(predictedTriple: PredictedTriple): Double
}
