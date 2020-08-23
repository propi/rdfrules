package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class OnlyBetterDescendant(measure: TypedKeyMap.Key[Measure]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = OnlyBetterDescendant

  def execute(input: Ruleset): Ruleset = {
    input.onlyBetterDescendant(measure)
  }
}

object OnlyBetterDescendant extends TaskDefinition {
  val name: String = "OnlyBetterDescendant"
}