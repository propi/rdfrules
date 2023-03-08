package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class Filter(nonEmptyTest: Boolean, nonEmptyPredictions: Boolean) extends Task[PredictionTasksResults, PredictionTasksResults] {
  val companion: TaskDefinition = Filter

  def execute(input: PredictionTasksResults): PredictionTasksResults = Function.chain[PredictionTasksResults](List(
    input => if (nonEmptyTest) input.nonEmptyTest() else input,
    input => if (nonEmptyPredictions) input.nonEmptyPredictions else input
  ))(input)
}

object Filter extends TaskDefinition {
  val name: String = "FilterPredictionTasks"
}
