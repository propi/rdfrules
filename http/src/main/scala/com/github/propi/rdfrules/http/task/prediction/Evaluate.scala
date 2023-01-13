package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class Evaluate(pca: Boolean, injectiveMapping: Boolean) extends Task[PredictedTriples, CompletenessEvaluationResult] {
  val companion: TaskDefinition = Evaluate

  def execute(input: PredictedTriples): CompletenessEvaluationResult = {
    input.evaluate(pca, injectiveMapping)
  }
}

object Evaluate extends TaskDefinition {
  val name: String = "Evaluate"
}