package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.prediction.PredictedTriples
import com.github.propi.rdfrules.utils.Debugger

class LoadPrediction(path: String, format: Option[ExportPrediction.Format])(implicit debugger: Debugger) extends Task[Index, PredictedTriples] {
  val companion: TaskDefinition = LoadPrediction

  def execute(input: Index): PredictedTriples = format match {
    case Some(ExportPrediction.Format.NonBinary(source)) => PredictedTriples(input, path)(source).withDebugger
    case Some(ExportPrediction.Format.Cache) => PredictedTriples.fromCache(input, path).withDebugger
    case None => PredictedTriples(input, path).withDebugger
  }

}

object LoadPrediction extends TaskDefinition {
  val name: String = "LoadPrediction"
}