package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictedTriples}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Predict(predictedResults: Set[PredictedResult], injectiveMapping: Boolean) extends Task[Ruleset, PredictedTriples] {
  val companion: TaskDefinition = Predict

  def execute(input: Ruleset): PredictedTriples = input.predict(predictedResults, injectiveMapping)

}

object Predict extends TaskDefinition {
  val name: String = "Predict"
}