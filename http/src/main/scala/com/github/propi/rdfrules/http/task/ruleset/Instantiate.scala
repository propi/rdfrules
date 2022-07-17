package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedResult
import com.github.propi.rdfrules.rule.ResolvedInstantiatedRule
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Instantiate(predictedResults: Set[PredictedResult], injectiveMapping: Boolean) extends Task[Ruleset, Seq[ResolvedInstantiatedRule]] {
  val companion: TaskDefinition = Instantiate

  def execute(input: Ruleset): Seq[ResolvedInstantiatedRule] = {
    input.instantiate(predictedResults, injectiveMapping).take(10000).resolvedRules.toSeq
  }
}

object Instantiate extends TaskDefinition {
  val name: String = "Instantiate"
}