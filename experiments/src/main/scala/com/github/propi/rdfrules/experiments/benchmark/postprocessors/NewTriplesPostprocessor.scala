package com.github.propi.rdfrules.experiments.benchmark.postprocessors

import com.github.propi.rdfrules.experiments.benchmark.{Metric, TaskPostProcessor}
import com.github.propi.rdfrules.prediction.PredictedResult
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 21. 5. 2019.
  */
trait NewTriplesPostprocessor extends TaskPostProcessor[Ruleset, Seq[Metric]] {

  val numberOfThreads: Int

  implicit val debugger: Debugger

  protected def postProcess(result: Ruleset): Seq[Metric] = {
    val rules = result.size
    val crules = result.setParallelism(numberOfThreads).computePcaConfidence(0.8).sorted.cache
    List(
      Metric.Number("rules", rules),
      Metric.Number("rulesConf", crules.size),
      Metric.Number("newTriples", crules.predict(Set(PredictedResult.PcaPositive)).distinctPredictions.triples.size)
    )
  }

}
