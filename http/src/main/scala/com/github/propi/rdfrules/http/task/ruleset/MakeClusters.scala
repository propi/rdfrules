package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class MakeClusters(clustering: Clustering[Rule.Simple]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = MakeClusters

  def execute(input: Ruleset): Ruleset = input.makeClusters(clustering)
}

object MakeClusters extends TaskDefinition {
  val name: String = "MakeClusters"
}