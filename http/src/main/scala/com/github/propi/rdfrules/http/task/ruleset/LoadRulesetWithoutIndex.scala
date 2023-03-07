package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.Debugger

class LoadRulesetWithoutIndex(path: String, format: RulesetSource, parallelism: Option[Int])(implicit debugger: Debugger) extends Task[Task.NoInput.type, Ruleset] {
  val companion: TaskDefinition = LoadRulesetWithoutIndex

  def execute(input: Task.NoInput.type): Ruleset = {
    val ruleset = Ruleset(Workspace.path(path))(Compression.fromPath(path) match {
      case Some(compression) => format.compressedBy(compression)
      case None => format
    })
    parallelism.map(ruleset.setParallelism).getOrElse(ruleset).withDebugger()
  }
}

object LoadRulesetWithoutIndex extends TaskDefinition {
  val name: String = "LoadRulesetWithoutIndex"
}