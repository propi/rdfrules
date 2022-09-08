package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionSource}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportPrediction(path: String, format: PredictionSource) extends Task[PredictedTriples, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportPrediction

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: PredictedTriples): Unit = input.export(Workspace.path(path))(Compression.fromPath(path) match {
    case Some(compression) => format.compressedBy(compression)
    case None => format
  })
}

object ExportPrediction extends TaskDefinition {
  val name: String = "ExportPrediction"
}

