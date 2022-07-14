package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GraphBasedRules extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = GraphBasedRules

  def execute(input: Ruleset): Ruleset = input.graphAwareRules
}

object GraphBasedRules extends TaskDefinition {
  val name: String = "GraphBasedRules"
}