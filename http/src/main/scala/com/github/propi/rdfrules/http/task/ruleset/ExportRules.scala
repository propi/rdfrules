package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportRules(path: String, format: RulesetSource) extends Task[Ruleset, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportRules

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path, false)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: Ruleset): Unit = input.export(Workspace.writablePath(path))(Compression.fromPath(path) match {
    case Some(compression) => format.compressedBy(compression)
    case None => format
  })
}

object ExportRules extends TaskDefinition {
  val name: String = "ExportRules"
}