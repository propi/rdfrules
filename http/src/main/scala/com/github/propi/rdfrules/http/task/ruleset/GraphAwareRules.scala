package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GraphAwareRules extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = GraphAwareRules

  def execute(input: Ruleset): Ruleset = input.graphAwareRules
}

object GraphAwareRules extends TaskDefinition {
  val name: String = "GraphAwareRules"
}