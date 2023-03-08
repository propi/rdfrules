package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.predictionTasks.Select.SelectionStrategy
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class Select(selectionStrategy: SelectionStrategy) extends Task[PredictionTasksResults, PredictionTasksResults] {
  val companion: TaskDefinition = Select

  def execute(input: PredictionTasksResults): PredictionTasksResults = selectionStrategy match {
    case SelectionStrategy.TopK(k) => input.topKPredictions(k)
    case SelectionStrategy.Pca => input.onlyPcaPredictions
    case SelectionStrategy.Qpca => input.onlyQpcaPredictions
  }
}

object Select extends TaskDefinition {
  val name: String = "SelectPredictions"

  sealed trait SelectionStrategy

  object SelectionStrategy {
    case class TopK(k: Int) extends SelectionStrategy

    case object Pca extends SelectionStrategy

    case object Qpca extends SelectionStrategy
  }
}