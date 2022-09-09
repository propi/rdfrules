package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{EvaluationResult, PredictedTriples}

class Evaluate(pca: Boolean, injectiveMapping: Boolean) extends Task[PredictedTriples, EvaluationResult] {
  val companion: TaskDefinition = Evaluate

  def execute(input: PredictedTriples): EvaluationResult = {
    input.evaluate(pca, injectiveMapping)
  }
}

object Evaluate extends TaskDefinition {
  val name: String = "Evaluate"
}