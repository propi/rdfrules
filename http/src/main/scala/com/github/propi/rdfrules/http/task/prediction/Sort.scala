package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class Sort() extends Task[PredictedTriples, PredictedTriples] {
  val companion: TaskDefinition = Sort

  def execute(input: PredictedTriples): PredictedTriples = input.sorted
}

object Sort extends TaskDefinition {
  val name: String = "SortPrediction"
}