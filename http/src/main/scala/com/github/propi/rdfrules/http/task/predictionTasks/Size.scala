package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class Size extends Task[PredictionTasksResults, Int] {
  val companion: TaskDefinition = Size

  def execute(input: PredictionTasksResults): Int = input.size
}

object Size extends TaskDefinition {
  val name: String = "PredictionTasksSize"
}