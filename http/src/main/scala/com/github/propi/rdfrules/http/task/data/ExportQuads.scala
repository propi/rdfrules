package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, RdfSource, RdfWriter}
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import org.apache.jena.riot.RDFFormat

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportQuads(path: String, format: Option[Either[RdfSource.Tsv.type, RDFFormat]]) extends Task[Dataset, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportQuads

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: Dataset): Unit = format match {
    case Some(x) =>
      implicit val writer: RdfWriter = x.fold(x => x, x => x)
      input.export(Workspace.path(path))
    case None => input.export(Workspace.path(path))
  }
}

object ExportQuads extends TaskDefinition {
  val name: String = "ExportQuads"
}