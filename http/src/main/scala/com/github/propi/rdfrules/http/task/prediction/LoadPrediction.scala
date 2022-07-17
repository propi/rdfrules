package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.prediction.PredictedTriples
import com.github.propi.rdfrules.utils.Debugger

class LoadPrediction(path: String)(implicit debugger: Debugger) extends Task[Index, PredictedTriples] {
  val companion: TaskDefinition = LoadPrediction

  def execute(input: Index): PredictedTriples = PredictedTriples.fromCache(input, path).withDebugger

}

object LoadPrediction extends TaskDefinition {
  val name: String = "LoadPrediction"
}