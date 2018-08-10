package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Slice(from: Int, until: Int) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Slice

  def execute(input: Ruleset): Ruleset = input.slice(from, until)
}

object Slice extends TaskDefinition {
  val name: String = "SliceRules"
}