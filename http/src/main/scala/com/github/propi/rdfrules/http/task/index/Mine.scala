package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.IndexPart
import com.github.propi.rdfrules.rule.RuleConstraint
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Mine(rulesMining: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset]) extends Task[IndexPart, Ruleset] {
  val companion: TaskDefinition = Mine

  def execute(input: IndexPart): Ruleset = input.mineRules(rulesMining.addConstraint(RuleConstraint.InjectiveMapping()), ruleConsumer).setParallelism(rulesMining.parallelism)
}

object Mine extends TaskDefinition {
  val name: String = "Mine"
}