package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.ForEach

import scala.collection.immutable.TreeMap

class PredictionTaskResult private(val predictionTask: PredictionTask, candidates: TreeMap[Int, (PredictedTriple, Int)]) {
  def predictedTriples: ForEach[PredictedTriple] = new ForEach[PredictedTriple] {
    def foreach(f: PredictedTriple => Unit): Unit = candidates.valuesIterator.map(_._1).foreach(f)

    override def knownSize: Int = candidates.size
  }

  def predictedCandidates: ForEach[Int] = new ForEach[Int] {
    def foreach(f: Int => Unit): Unit = candidates.keysIterator.foreach(f)

    override def knownSize: Int = candidates.size
  }

  def size: Int = candidates.size

  def correctPredictedTriplesRanks: Vector[Int] = predictedTriples.zipWithIndex.foldLeft(Vector.empty[Int]) { case (ranks, (predictedTriple, k)) =>
    if (predictedTriple.predictedResult == PredictedResult.Positive) {
      ranks :+ (k + 1 - ranks.length)
    } else {
      ranks
    }
  }

  def topCorrectPredictedTripleRank: Option[Int] = correctPredictedTriplesRanks.headOption

  def rank(candidate: Int): Option[Int] = candidates.get(candidate).map(_._2)

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

  def filterByQpca(implicit train: TripleIndex[Int]): PredictionTaskResult = {
    val currentCardinality = predictionTask.index.size(false)
    val maxCardinalityThreshold = train.predicates.get(predictionTask.p).map(x => if (predictionTask.targetVariable == TriplePosition.Subject) x.averageObjectCardinality else x.averageSubjectCardinality).getOrElse(1)
    val remainingSlots = maxCardinalityThreshold - currentCardinality
    if (remainingSlots > 0) {
      new PredictionTaskResult(predictionTask, candidates.take(remainingSlots))
    } else {
      new PredictionTaskResult(predictionTask, TreeMap.empty)
    }
  }

  def headOption: Option[PredictedTriple] = candidates.headOption.map(_._2._1)
}

object PredictionTaskResult {

  def apply(predictionTask: PredictionTask, predictedTriples: ForEach[PredictedTriple]): PredictionTaskResult = {
    val sortedMapBuilder = TreeMap.newBuilder[Int, (PredictedTriple, Int)]
    predictedTriples.zipWithIndex.map(x => (x._2 + 1) -> (x._1 -> (x._2 + 1))).foreach(sortedMapBuilder.addOne)
    new PredictionTaskResult(
      predictionTask,
      sortedMapBuilder.result()
    )
  }
}