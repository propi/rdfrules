package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{GroupedPredictedTriple, Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class GetPrediction(group: Boolean) extends Task[PredictedTriples, Seq[GroupedPredictedTriple]] {
  val companion: TaskDefinition = GetPrediction

  def execute(input: PredictedTriples): Seq[GroupedPredictedTriple] = {
    if (group) {
      input.resolvedTriples.map(GroupedPredictedTriple(_)).groupedBy(_.triple).map(_.reduce(_ :+ _.rules)).take(10000).toSeq
    } else {
      input.take(10000).resolvedTriples.map(GroupedPredictedTriple(_)).toSeq
    }
  }
}

object GetPrediction extends TaskDefinition {
  val name: String = "GetPrediction"
}