package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{Metric, Task, TaskPostProcessor, TaskPreProcessor}
import com.github.propi.rdfrules.prediction.PredictedResult
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class PruningRdfRules(val name: String) extends Task[Ruleset, Ruleset, (Ruleset, Ruleset), Seq[Metric]] with TaskPreProcessor[Ruleset, Ruleset] with TaskPostProcessor[(Ruleset, Ruleset), Seq[Metric]] {

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def postProcess(result: (Ruleset, Ruleset)): Seq[Metric] = {
    val (rules, pruned) = result
    List(
      Metric.Number("rules", rules.size),
      Metric.Number("prunedRules", pruned.size),
      Metric.Number("coveredTriples", rules.predict(Set(PredictedResult.Positive)).distinctPredictions.triples.size),
      Metric.Number("prunedCoveredTriples", pruned.predict(Set(PredictedResult.Positive)).distinctPredictions.triples.size)
    )
  }

  protected def taskBody(input: Ruleset): (Ruleset, Ruleset) = {
    val pruned = input.sorted.pruned(true, false).cache
    input -> pruned
  }

}