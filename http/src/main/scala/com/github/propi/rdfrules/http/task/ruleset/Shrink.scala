package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.data.Shrink.ShrinkSetup
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Shrink(shrinkSetup: ShrinkSetup) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Shrink

  def execute(input: Ruleset): Ruleset = shrinkSetup match {
    case ShrinkSetup.Drop(n) => input.drop(n)
    case ShrinkSetup.Take(n) => input.take(n)
    case ShrinkSetup.Slice(from, until) => input.slice(from, until)
  }
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkRules"
}