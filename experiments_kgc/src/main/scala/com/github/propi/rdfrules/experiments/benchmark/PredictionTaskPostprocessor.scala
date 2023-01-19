package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.prediction.PredictionTasksResults
import com.github.propi.rdfrules.prediction.eval.{CompletenessEvaluationBuilder, EvaluationResult, RankingEvaluationBuilder, StatsBuilder}
import com.github.propi.rdfrules.utils.ForEach

trait PredictionTaskPostprocessor extends TaskPostProcessor[PredictionTasksResults, Seq[Metric]] {
  private def processResults(prefix: String, results: List[EvaluationResult]): ForEach[Metric] = {
    ForEach.from(results).flatMap {
      case EvaluationResult.Stats(pt) => ForEach(Metric.Number(s"${prefix}PredictionTasks", pt))
      case EvaluationResult.Ranking(hits, mr, mrr, total, totalCorrect) => ForEach.from(hits).map(x => Metric.Number(s"${prefix}Hits${x.k}", x.value)).concat(ForEach(
        Metric.Number(s"${prefix}Mr", mr),
        Metric.Number(s"${prefix}Mrr", mrr),
        Metric.Number(s"${prefix}Total", total),
        Metric.Number(s"${prefix}TotalCorrect", totalCorrect)
      ))
      case x: EvaluationResult.Completeness => ForEach(
        Metric.Number(s"${prefix}Tp", x.tp),
        Metric.Number(s"${prefix}Fp", x.fp),
        Metric.Number(s"${prefix}Fn", x.fn),
        Metric.Number(s"${prefix}Precision", x.precision),
        Metric.Number(s"${prefix}Recall", x.recall),
        Metric.Number(s"${prefix}F1", x.fscore)
      )
    }
  }

  protected def postProcess(result: PredictionTasksResults): Seq[Metric] = {
    processResults("", result.evaluate(
      RankingEvaluationBuilder.fromTest(Vector(1, 3, 10)),
      StatsBuilder()
    )).concat(processResults("qpca", result.onlyQpcaPredictions.evaluate(CompletenessEvaluationBuilder())))
      .concat(processResults("pca", result.onlyFunctionalPredictions.evaluate(CompletenessEvaluationBuilder())))
      .concat(processResults("top1", result.map(_.topK(1)).evaluate(CompletenessEvaluationBuilder())))
      .concat(processResults("top2", result.map(_.topK(2)).evaluate(CompletenessEvaluationBuilder())))
      .concat(processResults("top3", result.map(_.topK(3)).evaluate(CompletenessEvaluationBuilder())))
      .concat(processResults("top4", result.map(_.topK(4)).evaluate(CompletenessEvaluationBuilder())))
      .toSeq
  }
}
