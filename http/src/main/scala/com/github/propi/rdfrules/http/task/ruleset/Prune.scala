package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Prune(onlyFunctionalProperties: Boolean) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Prune

  def execute(input: Ruleset): Ruleset = {
    input.pruned(onlyFunctionalProperties)
  }
}

object Prune extends TaskDefinition {
  val name: String = "Prune"
}