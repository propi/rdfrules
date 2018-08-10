package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GetRules extends Task[Ruleset, Traversable[ResolvedRule]] {
  val companion: TaskDefinition = GetRules

  def execute(input: Ruleset): Traversable[ResolvedRule] = input.take(10000).resolvedRules
}

object GetRules extends TaskDefinition {
  val name: String = "GetRules"
}