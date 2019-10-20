package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.model.LoadModel
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.RulesetSource

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class PredictTriples(path: String, format: Option[Option[RulesetSource]], onlyFunctionalProperties: Boolean) extends Task[Index, Dataset] {
  val companion: TaskDefinition = PredictTriples

  def execute(input: Index): Dataset = {
    val predictionResult = new LoadModel(Workspace.path(path), format).execute(Task.NoInput).completeIndex(input)
    if (onlyFunctionalProperties) predictionResult.onlyFunctionalProperties.graph.toDataset else predictionResult.graph.toDataset
  }
}

object PredictTriples extends TaskDefinition {
  val name: String = "PredictTriples"
}
