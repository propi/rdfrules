package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadRuleset(path: String) extends Task[Index, Ruleset] {
  val companion: TaskDefinition = LoadRuleset

  def execute(input: Index): Ruleset = Ruleset.fromCache(input, Workspace.path(path))
}

object LoadRuleset extends TaskDefinition {
  val name: String = "LoadRuleset"
}