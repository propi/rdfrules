package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.predictionTasks.Select.SelectionStrategy
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictionTasksResults}

class Select(selectionStrategy: Option[SelectionStrategy], minScore: Double, predictedResults: Set[PredictedResult]) extends Task[PredictionTasksResults, PredictionTasksResults] {
  val companion: TaskDefinition = Select

  def execute(input: PredictionTasksResults): PredictionTasksResults = Function.chain[PredictionTasksResults](List(
    input => if (predictedResults.isEmpty) input else input.map(_.filterCandidates(x => predictedResults(x.predictedResult))),
    input => if (minScore <= 0.0) input else input.map(_.filterCandidates(_.score >= minScore)),
    input => selectionStrategy.map {
      case SelectionStrategy.TopK(k) => input.topKPredictions(k)
      case SelectionStrategy.Pca => input.onlyPcaPredictions
      case SelectionStrategy.Qpca => input.onlyQpcaPredictions
    }.getOrElse(input)
  ))(input)
}

object Select extends TaskDefinition {
  val name: String = "SelectCandidates"

  sealed trait SelectionStrategy

  object SelectionStrategy {
    case class TopK(k: Int) extends SelectionStrategy

    case object Pca extends SelectionStrategy

    case object Qpca extends SelectionStrategy
  }
}