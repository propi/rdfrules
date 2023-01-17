package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.{DefaultConfidence, TripleItemPosition}
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.utils.{ForEach, TopKQueue}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import com.github.propi.rdfrules.serialization.TripleSerialization._

class PredictionTaskResult private(val predictionTask: PredictionTask, private val candidates: Iterable[PredictedTriple]) {
  def predictedTriples: ForEach[PredictedTriple] = candidates

  def predictedCandidates: ForEach[Int] = predictedTriples.map(_.triple.target(predictionTask.targetVariable))

  def size: Int = candidates.size

  def isEmpty: Boolean = candidates.isEmpty

  def correctPredictedTriplesRanks: Vector[Int] = predictedTriples.zipWithIndex.foldLeft(Vector.empty[Int]) { case (ranks, (predictedTriple, k)) =>
    if (predictedTriple.predictedResult == PredictedResult.Positive) {
      ranks :+ (k + 1 - ranks.length)
    } else {
      ranks
    }
  }

  def topCorrectPredictedTripleRank: Option[Int] = correctPredictedTriplesRanks.headOption

  def rank(candidate: Int): Option[Int] = predictedTriples
    .map(x => (x.predictedResult == PredictedResult.Positive) -> x.triple.target(predictionTask.targetVariable))
    .filter(x => !x._1 || x._2 == candidate)
    .zipWithIndex
    .find(_._1._2 == candidate)
    .map(_._2 + 1)

  def rank(triple: IntTriple): Option[Int] = {
    if (triple.p == predictionTask.p) {
      predictionTask.c match {
        case TripleItemPosition.Subject(s) if s == triple.s => rank(triple.o)
        case TripleItemPosition.Object(o) if o == triple.o => rank(triple.s)
        case _ => None
      }
    } else {
      None
    }
  }

  def ranks(injectiveMapping: Boolean)(implicit test: TripleIndex[Int]): Iterator[Option[Int]] = predictionTask.index.iterator(injectiveMapping).map(rank)

  def topK(k: Int): PredictionTaskResult = new PredictionTaskResult(predictionTask, candidates.view.take(k))

  def filterByFunctions(implicit train: TripleIndex[Int]): PredictionTaskResult = {
    if (predictionTask.index.isEmpty(false)) {
      topK(1)
    } else {
      new PredictionTaskResult(predictionTask, Nil)
    }
  }

  def filterByQpca(implicit train: TripleIndex[Int]): PredictionTaskResult = {
    val currentCardinality = predictionTask.index.size(false)
    val maxCardinalityThreshold = train.predicates.get(predictionTask.p).map(x => if (predictionTask.targetVariable == TriplePosition.Subject) x.averageObjectCardinality else x.averageSubjectCardinality).getOrElse(1)
    val remainingSlots = maxCardinalityThreshold - currentCardinality
    if (remainingSlots > 0) {
      topK(remainingSlots)
    } else {
      new PredictionTaskResult(predictionTask, Nil)
    }
  }

  def headOption: Option[PredictedTriple] = candidates.headOption
}

object PredictionTaskResult {

  def empty(predictionTask: PredictionTask): PredictionTaskResult = new PredictionTaskResult(predictionTask, Nil)

  def factory(topK: Int = -1)(implicit defaultConfidence: DefaultConfidence): collection.Factory[(PredictionTask, PredictedTriple), PredictionTaskResult] = new collection.Factory[(PredictionTask, PredictedTriple), PredictionTaskResult] {
    def fromSpecific(it: IterableOnce[(PredictionTask, PredictedTriple)]): PredictionTaskResult = it.iterator.foldLeft(newBuilder)(_.addOne(_)).result()

    def newBuilder: mutable.Builder[(PredictionTask, PredictedTriple), PredictionTaskResult] = {
      var predictionTask = Option.empty[PredictionTask]
      val predictedTriplesQueue = new TopKQueue[PredictedTriple](topK, false)
      new mutable.Builder[(PredictionTask, PredictedTriple), PredictionTaskResult] {
        def clear(): Unit = {
          predictionTask = None
          predictedTriplesQueue.clear()
        }

        def result(): PredictionTaskResult = new PredictionTaskResult(predictionTask.get, ArraySeq.from(predictedTriplesQueue.dequeueAll).view.reverse)

        def addOne(elem: (PredictionTask, PredictedTriple)): this.type = {
          if (predictionTask.isEmpty) predictionTask = Some(elem._1)
          predictedTriplesQueue.enqueue(elem._2)
          this
        }
      }
    }
  }

  implicit def predictionTaskResultSerializer(implicit mapper: TripleItemIndex): Serializer[PredictionTaskResult] = Serializer.by[PredictionTaskResult, (PredictionTask, Iterator[ResolvedPredictedTriple])](x => x.predictionTask -> x.candidates.iterator.map(ResolvedPredictedTriple(_)))

  implicit def predictionTaskResultDeserializer(implicit mapper: TripleItemIndex): Deserializer[PredictionTaskResult] = Deserializer.by[(PredictionTask, Iterable[ResolvedPredictedTriple]), PredictionTaskResult](x => new PredictionTaskResult(x._1, x._2.view.map(_.toPredictedTriple)))

}