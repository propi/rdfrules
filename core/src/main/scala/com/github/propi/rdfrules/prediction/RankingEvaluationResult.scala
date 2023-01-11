package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.prediction.RankingEvaluationResult.Hits

case class RankingEvaluationResult(hitsTotal: Seq[Hits], hitsCorrect: Seq[Hits], mr: Double, mrr: Double, predictionTasks: Int, correctPredictionTasks: Int)

object RankingEvaluationResult {
  case class Hits(k: Int, value: Double)
}