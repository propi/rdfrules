package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.prediction.{PredictedTriplesAggregator, PredictionTasksBuilder, PredictionTasksResults}
import com.github.propi.rdfrules.rule.DefaultConfidence
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class PredictionTask[T](val name: String, test: Dataset, scorer: PredictedTriplesAggregator.ScoreFactory, rulesAggregator: PredictedTriplesAggregator.RulesFactory)(implicit debugger: Debugger, defaultConfidence: DefaultConfidence) extends Task[Ruleset, Ruleset, PredictionTasksResults, T] with TaskPreProcessor[Ruleset, Ruleset] {
  self: TaskPostProcessor[PredictionTasksResults, T] =>

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def taskBody(input: Ruleset): PredictionTasksResults = {
    val res = input
      .predict(Some(test))
      .withoutTrainTriples
      .onlyCoveredTestPredictionTasks
      .grouped(scorer, rulesAggregator)
      .withDebugger("Predicted groupes", true)
      .predictionTasks(predictionTasksBuilder = PredictionTasksBuilder.FromTestSet.FromPredicateCardinalities, topK = 100)
      .withDebugger("Evaluation", true)
      .cache
    println(res.size)
    res
  }
}
