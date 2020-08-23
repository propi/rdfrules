package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{CoveredPaths, Ruleset}

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Instantiate(index: Int, part: CoveredPaths.Part) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Instantiate

  def execute(input: Ruleset): Ruleset = {
    input.slice(index, index).coveredPaths(part).headOption.map(_.paths).getOrElse(Ruleset(input.index, Nil, true))
  }
}

object Instantiate extends TaskDefinition {
  val name: String = "Instantiate"
}

