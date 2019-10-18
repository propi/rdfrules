package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sorted extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Sorted

  def execute(input: Ruleset): Ruleset = input.sorted
}

object Sorted extends TaskDefinition {
  val name: String = "Sorted"
}