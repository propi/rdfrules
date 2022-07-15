package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.InstantiatedRuleset
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Instantiate(predictedResults: Set[PredictedResult], injectiveMapping: Boolean) extends Task[Ruleset, InstantiatedRuleset] {
  val companion: TaskDefinition = Instantiate

  def execute(input: Ruleset): InstantiatedRuleset = {
    input.instantiate(predictedResults, injectiveMapping)
  }
}

object Instantiate extends TaskDefinition {
  val name: String = "Instantiate"
}