package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ComputePcaConfidence(min: Option[Double]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = ComputePcaConfidence

  def execute(input: Ruleset): Ruleset = input.computePcaConfidence(min.getOrElse(0.5))
}

object ComputePcaConfidence extends TaskDefinition {
  val name: String = "ComputePcaConfidence"
}