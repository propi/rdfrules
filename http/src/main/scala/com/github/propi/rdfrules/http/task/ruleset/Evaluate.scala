package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.EvaluationResult
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Evaluate(onlyFunctionalProperties: Boolean) extends Task[Ruleset, EvaluationResult] {
  val companion: TaskDefinition = Evaluate

  def execute(input: Ruleset): EvaluationResult = {
    val predictionResult = input.completeIndex
    if (onlyFunctionalProperties) predictionResult.onlyFunctionalProperties.evaluate(true) else predictionResult.evaluate(true)
  }
}

object Evaluate extends TaskDefinition {
  val name: String = "Evaluate"
}