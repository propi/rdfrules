package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.algorithm.dbscan.DbScan
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting.AtomsSimilarityCounting
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RulesetTask, TaskPostProcessor}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class ClusteringRdfRules[T](val name: String,
                            minNeigbours: Int,
                            minSimilarity: Double,
                            override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads)
                           (implicit val debugger: Debugger) extends RulesetTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def taskBody(input: Ruleset): Ruleset = {
    input.makeClusters(DbScan(minNeigbours, minSimilarity, numberOfThreads)(AtomsSimilarityCounting, debugger))
  }

}