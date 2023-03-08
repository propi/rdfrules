package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.prediction.{PredictionTaskResult, PredictionTasksResults}

class GetPredictionTasks extends Task[PredictionTasksResults, Seq[PredictionTaskResult.Resolved]] {
  val companion: TaskDefinition = GetPredictionTasks

  def execute(input: PredictionTasksResults): Seq[PredictionTaskResult.Resolved] = {
    implicit val mapper: TripleItemIndex = input.index.tripleItemMap
    input.take(10000).predictionTaskResults.map(PredictionTaskResult.Resolved(_)).toSeq
  }
}

object GetPredictionTasks extends TaskDefinition {
  val name: String = "GetPredictionTasks"
}