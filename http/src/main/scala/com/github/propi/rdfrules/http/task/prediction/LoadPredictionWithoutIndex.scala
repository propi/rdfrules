package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionSource}
import com.github.propi.rdfrules.utils.Debugger

class LoadPredictionWithoutIndex(path: String, format: PredictionSource)(implicit debugger: Debugger) extends Task[Task.NoInput.type, PredictedTriples] {
  val companion: TaskDefinition = LoadPredictionWithoutIndex

  def execute(input: Task.NoInput.type): PredictedTriples = PredictedTriples(Workspace.path(path))(Compression.fromPath(path) match {
    case Some(compression) => format.compressedBy(compression)
    case None => format
  }).withDebugger()

}

object LoadPredictionWithoutIndex extends TaskDefinition {
  val name: String = "LoadPredictionWithoutIndex"
}