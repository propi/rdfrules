package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.prediction.PredictionTasksResults

class ReadyPredictionTask[T](val name: String, f: PredictionTasksResults => PredictionTasksResults = x => x) extends Task[PredictionTasksResults, PredictionTasksResults, PredictionTasksResults, T] with TaskPreProcessor[PredictionTasksResults, PredictionTasksResults] {
  self: TaskPostProcessor[PredictionTasksResults, T] =>

  protected def taskBody(input: PredictionTasksResults): PredictionTasksResults = f(input)

  protected def preProcess(input: PredictionTasksResults): PredictionTasksResults = input
}
