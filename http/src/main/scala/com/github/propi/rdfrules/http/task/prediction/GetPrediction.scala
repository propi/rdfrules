package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, ResolvedPredictedTriple}

class GetPrediction(group: Boolean) extends Task[PredictedTriples, Seq[ResolvedPredictedTriple]] {
  val companion: TaskDefinition = GetPrediction

  def execute(input: PredictedTriples): Seq[ResolvedPredictedTriple] = {
    if (group) {
      input.grouped(10000).resolvedTriples.toSeq
    } else {
      input.take(10000).resolvedTriples.toSeq
    }
  }
}

object GetPrediction extends TaskDefinition {
  val name: String = "GetPrediction"
}