package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionTasksResults}

class ToPredictions extends Task[PredictionTasksResults, PredictedTriples] {
  val companion: TaskDefinition = ToPredictions

  def execute(input: PredictionTasksResults): PredictedTriples = input.predictedTriples
}

object ToPredictions extends TaskDefinition {
  val name: String = "PredictionTasksToPredictions"
}