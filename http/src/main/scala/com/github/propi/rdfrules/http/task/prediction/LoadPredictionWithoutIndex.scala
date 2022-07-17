package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples
import com.github.propi.rdfrules.utils.Debugger

class LoadPredictionWithoutIndex(path: String)(implicit debugger: Debugger) extends Task[Task.NoInput.type, PredictedTriples] {
  val companion: TaskDefinition = LoadPredictionWithoutIndex

  def execute(input: Task.NoInput.type): PredictedTriples = PredictedTriples.fromCache(path).withDebugger

}

object LoadPredictionWithoutIndex extends TaskDefinition {
  val name: String = "LoadPredictionWithoutIndex"
}