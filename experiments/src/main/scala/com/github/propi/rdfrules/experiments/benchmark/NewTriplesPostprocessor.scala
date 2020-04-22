package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 21. 5. 2019.
  */
trait NewTriplesPostprocessor extends TaskPostProcessor[Ruleset, Seq[Metric]] {

  val numberOfThreads: Int

  protected def postProcess(result: Ruleset): Seq[Metric] = {
    val rules = result.size
    val crules = result.setParallelism(numberOfThreads).computeConfidence(0.5).sorted.cache
    List(
      Metric.Number("rules", rules),
      Metric.Number("rulesConf", crules.size),
      Metric.Number("newTriples", crules.predictedTriples(PredictionType.Missing).distinct.triples.size)
    )
  }

}
