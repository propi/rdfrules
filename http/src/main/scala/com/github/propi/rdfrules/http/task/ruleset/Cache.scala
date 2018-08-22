package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Cache

  def execute(input: Ruleset): Ruleset = input.cache(Workspace.path(path))
}

object Cache extends TaskDefinition {
  val name: String = "CacheRuleset"
}