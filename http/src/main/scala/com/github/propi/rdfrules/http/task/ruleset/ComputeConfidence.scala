package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ComputeConfidence(min: Option[Double]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = ComputeConfidence

  def execute(input: Ruleset): Ruleset = input.computeConfidence(min.getOrElse(0.5))
}

object ComputeConfidence extends TaskDefinition {
  val name: String = "ComputeConfidence"
}