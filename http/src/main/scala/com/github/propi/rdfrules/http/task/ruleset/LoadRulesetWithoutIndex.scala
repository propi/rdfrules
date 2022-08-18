package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.Debugger

class LoadRulesetWithoutIndex(path: String, format: Option[RulesetSource], parallelism: Option[Int])(implicit debugger: Debugger) extends Task[Task.NoInput.type, Ruleset] {
  val companion: TaskDefinition = LoadRulesetWithoutIndex

  def execute(input: Task.NoInput.type): Ruleset = {
    val ruleset = format match {
      case Some(source) => Ruleset(Workspace.path(path))(source)
      case None => Ruleset.fromCache(Workspace.path(path))
    }
    parallelism.map(ruleset.setParallelism).getOrElse(ruleset).withDebugger
  }
}

object LoadRulesetWithoutIndex extends TaskDefinition {
  val name: String = "LoadRulesetWithoutIndex"
}