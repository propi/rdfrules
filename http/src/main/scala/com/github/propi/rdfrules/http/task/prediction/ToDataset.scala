package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class ToDataset extends Task[PredictedTriples, Dataset] {
  val companion: TaskDefinition = ToDataset

  def execute(input: PredictedTriples): Dataset = input.toGraph.toDataset
}

object ToDataset extends TaskDefinition {
  val name: String = "PredictionToDataset"
}