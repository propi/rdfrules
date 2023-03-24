package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition, TripleMatcher}
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictionTasksResults}

class Filter(predictedResults: Set[PredictedResult],
             tripleMatchers: Seq[TripleMatcher],
             nonEmptyPredictions: Boolean) extends Task[PredictionTasksResults, PredictionTasksResults] {
  val companion: TaskDefinition = Filter

  def execute(input: PredictionTasksResults): PredictionTasksResults = Function.chain[PredictionTasksResults](List(
    input => if (predictedResults.isEmpty) input else input.filter(x => x.predictedTriples.exists(x => predictedResults(x.predictedResult))),
    input => if (tripleMatchers.isEmpty) input else input.filterResolved(x => tripleMatchers.exists(_.matchAll(x.predictionTask.toTriple).matched)),
    input => if (nonEmptyPredictions) input.nonEmptyPredictions else input
  ))(input)
}

object Filter extends TaskDefinition {
  val name: String = "FilterPredictionTasks"
}
