package com.github.propi.rdfrules.experiments.benchmark
import com.github.propi.rdfrules.prediction.PredictionTasksResults

trait ModesPredictionTaskPostprocessor extends PredictionTaskPostprocessor {
  override protected def postProcess(result: PredictionTasksResults): Seq[Metric] = super.postProcess(result.withAddedModePredictions())
}