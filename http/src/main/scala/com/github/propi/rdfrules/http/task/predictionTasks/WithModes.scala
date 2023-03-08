package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class WithModes() extends Task[PredictionTasksResults, PredictionTasksResults] {
  val companion: TaskDefinition = WithModes

  def execute(input: PredictionTasksResults): PredictionTasksResults = input.withAddedModePredictions()
}

object WithModes extends TaskDefinition {
  val name: String = "WithModes"
}


