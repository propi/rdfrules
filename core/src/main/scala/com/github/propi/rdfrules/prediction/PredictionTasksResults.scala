package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.ops.{Debugable, Transformable}
import com.github.propi.rdfrules.index.TrainTestIndex
import com.github.propi.rdfrules.prediction.eval.{EvaluationBuilder, EvaluationResult}
import com.github.propi.rdfrules.utils.ForEach

class PredictionTasksResults private(protected val coll: ForEach[PredictionTaskResult], val parallelism: Int)(implicit val index: TrainTestIndex)
  extends Transformable[PredictionTaskResult, PredictionTasksResults] with Debugable[PredictionTaskResult, PredictionTasksResults] {

  private def createPredictedTriples(predictedTriples: ForEach[PredictedTriple]) = PredictedTriples(index, predictedTriples).setParallelism(parallelism)

  protected def dataLoadingText: String = "Prediction tasks processing"

  protected def transform(col: ForEach[PredictionTaskResult]): PredictionTasksResults = new PredictionTasksResults(col, parallelism)

  def resolvedPredictionTasksResults: ForEach[(PredictionTask.Resolved, PredictedTriples)] = coll.map(x => PredictionTask.Resolved(x.predictionTask)(index.test.tripleItemMap) -> createPredictedTriples(x.predictedTriples))

  def nonEmptyPredictions: PredictionTasksResults = filter(!_.isEmpty)

  def nonEmptyTest(injectiveMapping: Boolean = true): PredictionTasksResults = filter(x => !x.predictionTask.index(index.test.tripleMap).isEmpty(injectiveMapping))

  def evaluate(evaluator: EvaluationBuilder, evaluators: EvaluationBuilder*): List[EvaluationResult] = {
    for (predictionTaskResult <- coll) {
      evaluator.evaluate(predictionTaskResult)(index.test.tripleMap)
      evaluators.foreach(_.evaluate(predictionTaskResult)(index.test.tripleMap))
    }
    List.from(Iterator(evaluator.build) ++ evaluators.iterator.map(_.build))
  }

  def onlyFunctionalPredictions: PredictedTriples = createPredictedTriples(coll.flatMap(_.headOption))

  def onlyQpcaPredictions: PredictedTriples = createPredictedTriples(coll.flatMap(_.filterByQpca(index.train.tripleMap).predictedTriples))
}

object PredictionTasksResults {
  def apply(coll: ForEach[PredictionTaskResult], parallelism: Int)(implicit index: TrainTestIndex): PredictionTasksResults = new PredictionTasksResults(coll, parallelism)(index)
}