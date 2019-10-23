package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class PredictTriples(onlyFunctionalProperties: Boolean) extends Task[Ruleset, Dataset] {
  val companion: TaskDefinition = PredictTriples

  def execute(input: Ruleset): Dataset = {
    val predictionResult = input.completeIndex
    if (onlyFunctionalProperties) predictionResult.onlyFunctionalProperties.graph.toDataset else predictionResult.graph.toDataset
  }
}

object PredictTriples extends TaskDefinition {
  val name: String = "PredictTriples"
}