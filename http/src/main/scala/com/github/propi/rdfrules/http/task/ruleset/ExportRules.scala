package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource, RulesetWriter}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportRules(path: String, format: Option[RulesetSource]) extends Task[Ruleset, Unit] {
  val companion: TaskDefinition = ExportRules

  def execute(input: Ruleset): Unit = format match {
    case Some(x) =>
      implicit val writer: RulesetWriter = x match {
        case x: RulesetSource.Text.type => x
        case x: RulesetSource.Json.type => x
      }
      input.export(path)
    case None => input.export(path)
  }
}

object ExportRules extends TaskDefinition {
  val name: String = "ExportRules"
}