package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{TrainTestIndex, TripleItemIndex}
import com.github.propi.rdfrules.prediction.eval.{EvaluationBuilder, EvaluationResult}
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

class PredictionTasksResults private(protected val coll: ForEach[PredictionTaskResult], val parallelism: Int)(implicit val index: TrainTestIndex)
  extends Transformable[PredictionTaskResult, PredictionTasksResults] with Debugable[PredictionTaskResult, PredictionTasksResults] with Cacheable[PredictionTaskResult, PredictionTasksResults] {

  private def createPredictedTriples(predictedTriples: ForEach[PredictedTriple]) = PredictedTriples(index, predictedTriples).setParallelism(parallelism)

  implicit private def mapper: TripleItemIndex = index.test.tripleItemMap

  protected def dataLoadingText: String = "Prediction tasks processing"

  protected def transform(col: ForEach[PredictionTaskResult]): PredictionTasksResults = new PredictionTasksResults(col, parallelism)

  protected def cachedTransform(col: ForEach[PredictionTaskResult]): PredictionTasksResults = transform(col)

  protected def serializer: Serializer[PredictionTaskResult] = implicitly[Serializer[PredictionTaskResult]]

  protected def deserializer: Deserializer[PredictionTaskResult] = implicitly[Deserializer[PredictionTaskResult]]

  protected implicit def serializationSize: SerializationSize[PredictionTaskResult] = implicitly[SerializationSize[PredictionTaskResult]]

  def predictedTriples: PredictedTriples = createPredictedTriples(coll.flatMap(_.predictedTriples))

  def predictionTaskResults: ForEach[PredictionTaskResult] = coll

  def resolvedPredictionTasksResults: ForEach[(PredictionTask.Resolved, PredictedTriples)] = coll.map(x => PredictionTask.Resolved(x.predictionTask)(index.test.tripleItemMap) -> createPredictedTriples(x.predictedTriples))

  def nonEmptyPredictions: PredictionTasksResults = filter(!_.isEmpty)

  def withAddedModePredictions(injectiveMapping: Boolean = true): PredictionTasksResults = map(_.withAddedModePrediction(injectiveMapping))

  def nonEmptyTest(injectiveMapping: Boolean = true): PredictionTasksResults = filter(x => !x.predictionTask.index(index.test.tripleMap).isEmpty(injectiveMapping))

  def evaluate(evaluator: EvaluationBuilder, evaluators: EvaluationBuilder*): List[EvaluationResult] = {
    for (predictionTaskResult <- coll) {
      evaluator.evaluate(predictionTaskResult)(index.test.tripleMap)
      evaluators.foreach(_.evaluate(predictionTaskResult)(index.test.tripleMap))
    }
    List.from(Iterator(evaluator.build) ++ evaluators.iterator.map(_.build))
  }

  def onlyFunctionalPredictions: PredictionTasksResults = this.map(_.filterByFunctions(index.train.tripleMap))

  def onlyQpcaPredictions: PredictionTasksResults = this.map(_.filterByQpca)
}

object PredictionTasksResults {
  def apply(coll: ForEach[PredictionTaskResult], parallelism: Int)(implicit index: TrainTestIndex): PredictionTasksResults = new PredictionTasksResults(coll, parallelism)(index)
}