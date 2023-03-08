package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionTasksBuilder, PredictionTasksResults}
import com.github.propi.rdfrules.rule.DefaultConfidence
import com.github.propi.rdfrules.utils.Debugger

class ToPredictionTasks(predictionTasksBuilder: PredictionTasksBuilder, limit: Int, topK: Int)(implicit defaultConfidence: DefaultConfidence, debugger: Debugger) extends Task[PredictedTriples, PredictionTasksResults] {
  val companion: TaskDefinition = ToPredictionTasks

  def execute(input: PredictedTriples): PredictionTasksResults = input.predictionTasks(predictionTasksBuilder, limit, topK)
}

object ToPredictionTasks extends TaskDefinition {
  val name: String = "ToPredictionTasks"
}


