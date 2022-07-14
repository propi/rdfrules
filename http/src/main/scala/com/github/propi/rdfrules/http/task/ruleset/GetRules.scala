package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GetRules extends Task[Ruleset, Seq[ResolvedRule]] {
  val companion: TaskDefinition = GetRules

  def execute(input: Ruleset): Seq[ResolvedRule] = input.take(10000).resolvedRules.toSeq
}

object GetRules extends TaskDefinition {
  val name: String = "GetRules"
}