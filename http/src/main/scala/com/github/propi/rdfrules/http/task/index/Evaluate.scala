package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.task.model.LoadModel
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.EvaluationResult
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.ruleset.RulesetSource
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Evaluate(path: String, format: Option[Option[RulesetSource]], onlyFunctionalProperties: Boolean)(implicit debugger: Debugger) extends Task[Index, EvaluationResult] {
  val companion: TaskDefinition = Evaluate

  def execute(input: Index): EvaluationResult = {
    val predictionResult = new LoadModel(path, format).execute(Task.NoInput).predictForIndex(input, PredictionType.All)
    if (onlyFunctionalProperties) predictionResult.onlyFunctionalProperties.evaluate(true) else predictionResult.evaluate(true)
  }
}

object Evaluate extends TaskDefinition {
  val name: String = "Evaluate"
}