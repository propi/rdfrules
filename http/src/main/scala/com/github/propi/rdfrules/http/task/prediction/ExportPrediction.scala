package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictionSource}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportPrediction(path: String, format: Option[ExportPrediction.Format]) extends Task[PredictedTriples, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportPrediction

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: PredictedTriples): Unit = format match {
    case Some(ExportPrediction.Format.NonBinary(x)) => input.export(Workspace.path(path))(x)
    case Some(ExportPrediction.Format.Cache) => input.cache(Workspace.path(path))
    case None => input.export(Workspace.path(path))
  }
}

object ExportPrediction extends TaskDefinition {
  val name: String = "ExportPrediction"

  sealed trait Format

  object Format {
    case class NonBinary(source: PredictionSource) extends Format

    case object Cache extends Format
  }
}

