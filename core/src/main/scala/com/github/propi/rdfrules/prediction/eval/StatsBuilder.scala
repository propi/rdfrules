package com.github.propi.rdfrules.prediction.eval
import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.prediction.PredictionTaskResult

class StatsBuilder private extends EvaluationBuilder {
  var i = 0

  def evaluate(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): Unit = i += 1

  def build: EvaluationResult = EvaluationResult.Stats(i)
}

object StatsBuilder {
  def apply(): StatsBuilder = new StatsBuilder()
}