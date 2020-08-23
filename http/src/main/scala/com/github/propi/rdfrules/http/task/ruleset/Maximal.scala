package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Maximal extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Maximal

  def execute(input: Ruleset): Ruleset = {
    input.maximal
  }
}

object Maximal extends TaskDefinition {
  val name: String = "Maximal"
}