package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource, RulesetWriter}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportRules(path: String, format: Option[RulesetSource]) extends Task[Model, Unit] {
  val companion: TaskDefinition = ExportRules

  def execute(input: Model): Unit = format match {
    case Some(x) =>
      implicit val writer: RulesetWriter = x match {
        case x: RulesetSource.Text.type => x
        case x: RulesetSource.Json.type => x
      }
      input.export(Workspace.path(path))
    case None => input.export(Workspace.path(path))
  }
}

object ExportRules extends TaskDefinition {
  val name: String = "ExportRules"
}