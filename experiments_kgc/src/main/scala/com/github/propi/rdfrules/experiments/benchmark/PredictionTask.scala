package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.prediction.aggregator.TopRules
import com.github.propi.rdfrules.prediction.{PredictedTriplesAggregator, PredictionTasksBuilder, PredictionTasksResults}
import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class PredictionTask[T](val name: String, test: Dataset, scorer: PredictedTriplesAggregator.ScoreFactory)(implicit debugger: Debugger, defaultConfidence: DefaultConfidence) extends Task[Ruleset, Ruleset, PredictionTasksResults, T] with TaskPreProcessor[Ruleset, Ruleset] {
  self: TaskPostProcessor[PredictionTasksResults, T] =>

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def taskBody(input: Ruleset): PredictionTasksResults = {
    implicit val _confidence: Measure.Confidence[Measure.ConfidenceMeasure] = defaultConfidence.confidenceType.get
    input
      .withoutQuasiBinding()
      .computeConfidence(0.1)
      .onlyBetterDescendant(_confidence)
      .computeLift()
      .filter(_.measures.apply[Measure.Lift].value > 1.0)
      .sorted
      .cache
      .withDebugger("Rules predicted", true)
      .predict(Some(test)).withoutTrainTriples.withCoveredTestPredictionTasks
      .grouped(scorer, TopRules(100))
      .withDebugger("Predicted groupes", true)
      .predictionTasks(predictionTasksBuilder = PredictionTasksBuilder.FromTestSet.FromPredicateCardinalities, topK = 100)
      .withDebugger("Evaluation", true)
      .cache
  }
}
