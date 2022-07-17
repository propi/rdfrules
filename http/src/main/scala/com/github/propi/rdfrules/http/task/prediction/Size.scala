package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class Size extends Task[PredictedTriples, Int] {
  val companion: TaskDefinition = Size

  def execute(input: PredictedTriples): Int = input.size
}

object Size extends TaskDefinition {
  val name: String = "PredictionSize"
}