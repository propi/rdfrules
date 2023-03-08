package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.predictionTasks.Evaluate.RankingStrategy
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults
import com.github.propi.rdfrules.prediction.eval.{CompletenessEvaluationBuilder, EvaluationResult, RankingEvaluationBuilder, StatsBuilder}

class Evaluate(rankingStrategy: RankingStrategy) extends Task[PredictionTasksResults, Seq[EvaluationResult]] {
  val companion: TaskDefinition = Evaluate

  def execute(input: PredictionTasksResults): Seq[EvaluationResult] = {
    input.evaluate(rankingStrategy match {
      case RankingStrategy.FromTest => RankingEvaluationBuilder.fromTest(Vector(1, 3, 10, 100))
      case RankingStrategy.FromPrediction => RankingEvaluationBuilder.fromPrediction(Vector(1, 3, 10, 100))
    }, CompletenessEvaluationBuilder(), StatsBuilder())
  }
}

object Evaluate extends TaskDefinition {
  val name: String = "Evaluate"

  sealed trait RankingStrategy

  object RankingStrategy {
    case object FromPrediction extends RankingStrategy

    case object FromTest extends RankingStrategy
  }
}