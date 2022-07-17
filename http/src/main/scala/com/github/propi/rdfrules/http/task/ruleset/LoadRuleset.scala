package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadRuleset(path: String, format: Option[RulesetSource], parallelism: Option[Int])(implicit debugger: Debugger) extends Task[Index, Ruleset] {
  val companion: TaskDefinition = LoadRuleset

  def execute(input: Index): Ruleset = {
    val ruleset = format match {
      case Some(source) => Ruleset(input, Workspace.path(path))(source)
      case None => Ruleset.fromCache(input, Workspace.path(path))
    }
    parallelism.map(ruleset.setParallelism).getOrElse(ruleset).withDebugger
  }
}

object LoadRuleset extends TaskDefinition {
  val name: String = "LoadRuleset"
}