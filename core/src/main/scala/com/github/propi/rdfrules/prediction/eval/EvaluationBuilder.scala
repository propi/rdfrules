package com.github.propi.rdfrules.prediction.eval

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.prediction.PredictionTaskResult

trait EvaluationBuilder {
  def evaluate(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): Unit

  def build: EvaluationResult
}