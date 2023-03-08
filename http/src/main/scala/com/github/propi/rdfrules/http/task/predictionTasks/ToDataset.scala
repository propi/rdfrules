package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class ToDataset extends Task[PredictionTasksResults, Dataset] {
  val companion: TaskDefinition = ToDataset

  def execute(input: PredictionTasksResults): Dataset = input.predictedTriples.toGraph.toDataset
}

object ToDataset extends TaskDefinition {
  val name: String = "PredictionTasksToDataset"
}