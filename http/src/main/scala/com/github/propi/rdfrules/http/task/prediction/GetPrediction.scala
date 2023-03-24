package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, ResolvedPredictedTriple}

class GetPrediction(maxRules: Int) extends Task[PredictedTriples, Seq[ResolvedPredictedTriple]] {
  val companion: TaskDefinition = GetPrediction

  def execute(input: PredictedTriples): Seq[ResolvedPredictedTriple] = input
    .resolvedTriples
    .map(x => ResolvedPredictedTriple(x.triple, x.predictedResult, x.rules.view.take(maxRules).toIndexedSeq, x.score))
    .scanLeft(0) { (totalTriples, x) =>
      (totalTriples + x.rules.size) -> x
    }
    .takeWhile(_._1 <= 10000)
    .map(_._2)
    .toSeq
}

object GetPrediction extends TaskDefinition {
  val name: String = "GetPrediction"
}