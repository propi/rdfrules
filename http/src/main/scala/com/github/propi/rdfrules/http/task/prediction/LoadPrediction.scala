package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionSource}
import com.github.propi.rdfrules.utils.Debugger

class LoadPrediction(path: String, format: PredictionSource)(implicit debugger: Debugger) extends Task[Index, PredictedTriples] {
  val companion: TaskDefinition = LoadPrediction

  def execute(input: Index): PredictedTriples = PredictedTriples(input, Workspace.path(path))(Compression.fromPath(path) match {
    case Some(compression) => format.compressedBy(compression)
    case None => format
  }).withDebugger

}

object LoadPrediction extends TaskDefinition {
  val name: String = "LoadPrediction"
}