package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictedTriples}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Predict(testSet: Option[String], mergeTestAndTrainForPrediction: Boolean, onlyTestCoveredPredictions: Boolean, predictedResults: Set[PredictedResult], injectiveMapping: Boolean)(implicit debugger: Debugger) extends Task[Ruleset, PredictedTriples] {
  val companion: TaskDefinition = Predict

  def execute(input: Ruleset): PredictedTriples = {
    val testDataset = testSet.map(path => Dataset(Workspace.path(path)))
    input.predict(testDataset, mergeTestAndTrainForPrediction, onlyTestCoveredPredictions, predictedResults, injectiveMapping)
  }

}

object Predict extends TaskDefinition {
  val name: String = "Predict"
}