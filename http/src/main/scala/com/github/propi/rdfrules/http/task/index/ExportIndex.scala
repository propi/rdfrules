package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.index.IndexPart

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportIndex(path: String) extends Task[IndexPart, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportIndex

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: IndexPart): Unit = input.cache(Workspace.path(path))
}

object ExportIndex extends TaskDefinition {
  val name: String = "ExportIndex"
}