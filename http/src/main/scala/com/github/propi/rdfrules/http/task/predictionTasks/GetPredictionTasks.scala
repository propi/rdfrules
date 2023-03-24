package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.prediction.{PredictionTaskResult, PredictionTasksResults, ResolvedPredictedTriple}

class GetPredictionTasks(maxCandidates: Int) extends Task[PredictionTasksResults, Seq[PredictionTaskResult.Resolved]] {
  val companion: TaskDefinition = GetPredictionTasks

  def execute(input: PredictionTasksResults): Seq[PredictionTaskResult.Resolved] = {
    implicit val mapper: TripleItemIndex = input.index.tripleItemMap
    input
      .resolvedPredictionTasksResults
      .map(x => x.copy(candidates = x.candidates.iterator.map(x => ResolvedPredictedTriple(x.triple, x.predictedResult, Nil, x.score)).take(maxCandidates).toIndexedSeq))
      .scanLeft(0) { (totalCandidates, x) =>
        val size = x.candidates.size
        (totalCandidates + (if (size == 0) 1 else size)) -> x
      }
      .takeWhile(_._1 <= 10000)
      .map(_._2)
      .toSeq
  }
}

object GetPredictionTasks extends TaskDefinition {
  val name: String = "GetPredictionTasks"
}