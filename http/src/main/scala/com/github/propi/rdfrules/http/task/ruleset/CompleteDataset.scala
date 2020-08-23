package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class CompleteDataset(onlyFunctionalProperties: Boolean, onlyNewPredicates: Boolean) extends Task[Ruleset, Dataset] {
  val companion: TaskDefinition = CompleteDataset

  def execute(input: Ruleset): Dataset = {
    val predictionResult = input.predictedTriples(if (onlyNewPredicates) PredictionType.Complementary else PredictionType.Missing)
    if (onlyFunctionalProperties) predictionResult.onlyFunctionalProperties.mergedDataset else predictionResult.mergedDataset
  }
}

object CompleteDataset extends TaskDefinition {
  val name: String = "CompleteDataset"
}