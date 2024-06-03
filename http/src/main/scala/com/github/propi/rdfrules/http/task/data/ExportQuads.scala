package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportQuads(path: String) extends Task[Dataset, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportQuads

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path, false)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: Dataset): Unit = input.export(Workspace.writablePath(path))
}

object ExportQuads extends TaskDefinition {
  val name: String = "ExportQuads"
}