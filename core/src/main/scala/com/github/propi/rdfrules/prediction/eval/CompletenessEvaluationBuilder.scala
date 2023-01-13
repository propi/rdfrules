package com.github.propi.rdfrules.prediction.eval

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.prediction.PredictionTaskResult

trait CompletenessEvaluationBuilder extends EvaluationBuilder {
  def build: EvaluationResult.Completeness
}

object CompletenessEvaluationBuilder {

  private class Basic(injectiveMapping: Boolean) extends CompletenessEvaluationBuilder {
    private var actual = 0
    private var predicted = 0
    private var tp = 0

    def evaluate(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): Unit = {
      for (actualTarget <- predictionTaskResult.predictionTask.index.iterator(injectiveMapping)) {
        actual += 1
        if (predictionTaskResult.rank(actualTarget).isDefined) tp += 1
      }
      predicted += predictionTaskResult.size
    }

    def build: EvaluationResult.Completeness = EvaluationResult.Completeness(tp, predicted - tp, actual - tp, 0)
  }

  def apply(injectiveMapping: Boolean = true): CompletenessEvaluationBuilder = new Basic(injectiveMapping)

}