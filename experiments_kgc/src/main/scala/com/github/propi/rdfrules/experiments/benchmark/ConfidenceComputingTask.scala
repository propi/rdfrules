package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class ConfidenceComputingTask(val name: String, exportPath: String)(implicit debugger: Debugger, defaultConfidence: DefaultConfidence) extends Task[Ruleset, Ruleset, Ruleset, Ruleset] with TaskPreProcessor[Ruleset, Ruleset] with TaskPostProcessor[Ruleset, Ruleset] {
  protected def taskBody(input: Ruleset): Ruleset = {
    implicit val _confidence: Measure.Confidence[Measure.ConfidenceMeasure] = defaultConfidence.confidenceType.get
    input.withoutQuasiBinding()
      .computeConfidence(0.1, topK = 1000000)
      .onlyBetterDescendant(_confidence)
      .computeLift()
      .filter(_.measures.apply[Measure.Lift].value > 1.0)
      .sorted
      .cache
  }

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def postProcess(result: Ruleset): Ruleset = {
    result.`export`(exportPath)
    result
  }
}
