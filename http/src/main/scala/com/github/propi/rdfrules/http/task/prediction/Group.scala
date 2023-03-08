package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.{PredictedTriples, PredictedTriplesAggregator}

class Group(scorer: PredictedTriplesAggregator.ScoreFactory, consumer: PredictedTriplesAggregator.RulesFactory, limit: Int) extends Task[PredictedTriples, PredictedTriples] {
  val companion: TaskDefinition = Group

  def execute(input: PredictedTriples): PredictedTriples = input.grouped(scorer, consumer, limit)
}

object Group extends TaskDefinition {
  val name: String = "GroupPredictions"
}
