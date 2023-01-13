package com.github.propi.rdfrules.prediction.eval

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.prediction.PredictionTaskResult

sealed trait RankingEvaluationBuilder extends EvaluationBuilder {
  val hitsK: IndexedSeq[Int]

  private var q = 0
  private var qr = 0
  private val sumHits = Array.fill(hitsK.length)(0)
  private var sumRank = 0
  private var sumiRank = 0.0

  protected def predictionTaskResultToRanks(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): IterableOnce[Int]

  final def evaluate(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): Unit = {
    for (rank <- predictionTaskResultToRanks(predictionTaskResult).iterator) {
      for (i <- hitsK.indices if rank <= hitsK(i) && rank > 0) {
        sumHits(i) += 1
      }
      q += 1
      if (rank > 0) {
        qr += 1
        sumRank += rank
        sumiRank += 1.0 / rank
      }
    }
  }

  final def build: EvaluationResult.Ranking = {
    val qDouble = q.toDouble
    val qrDouble = qr.toDouble
    EvaluationResult.Ranking(
      if (q == 0) hitsK.map(k => HitsK(k, 0.0)) else hitsK.iterator.zipWithIndex.map { case (k, i) => HitsK(k, sumHits(i) / qDouble) }.toSeq,
      if (qr == 0) 0.0 else sumRank / qrDouble,
      if (q == 0) 0.0 else sumiRank / qDouble,
      q,
      qr
    )
  }
}

object RankingEvaluationBuilder {

  private class FromTest(val hitsK: IndexedSeq[Int], injectiveMapping: Boolean) extends RankingEvaluationBuilder {
    protected def predictionTaskResultToRanks(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): IterableOnce[Int] = {
      predictionTaskResult.ranks(injectiveMapping).map(_.getOrElse(0))
    }
  }

  private class FromPrediction(val hitsK: IndexedSeq[Int]) extends RankingEvaluationBuilder {
    protected def predictionTaskResultToRanks(predictionTaskResult: PredictionTaskResult)(implicit test: TripleIndex[Int]): IterableOnce[Int] = {
      val _ranks = predictionTaskResult.correctPredictedTriplesRanks
      if (_ranks.isEmpty) List(0) else _ranks
    }
  }

  def fromTest(hitsK: IndexedSeq[Int], injectiveMapping: Boolean = true): RankingEvaluationBuilder = new FromTest(hitsK, injectiveMapping)

  def fromPrediction(hitsK: IndexedSeq[Int]): RankingEvaluationBuilder = new FromPrediction(hitsK)

}