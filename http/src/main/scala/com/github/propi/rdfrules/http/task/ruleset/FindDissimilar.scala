package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class FindDissimilar(resolvedRule: ResolvedRule, k: Int) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = FindDissimilar

  def execute(input: Ruleset): Ruleset = input.findDissimilar(resolvedRule, k)
}

object FindDissimilar extends TaskDefinition {
  val name: String = "FindDissimilar"
}