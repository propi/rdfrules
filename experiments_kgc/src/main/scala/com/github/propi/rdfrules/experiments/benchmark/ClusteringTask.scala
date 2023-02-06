package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting.BodyAtomsSimilarityCounting
import com.github.propi.rdfrules.algorithm.clustering.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class ClusteringTask(val name: String, exportPath: String)(implicit debugger: Debugger) extends Task[Ruleset, Ruleset, Ruleset, Ruleset] with TaskPreProcessor[Ruleset, Ruleset] with TaskPostProcessor[Ruleset, Ruleset] {
  protected def taskBody(input: Ruleset): Ruleset = {
    implicit val similarityCounting: SimilarityCounting[FinalRule] = BodyAtomsSimilarityCounting
    val res = input.makeClusters(DbScan(1, 0.8)).cache
    println(s"number of clusters: ${res.rules.map(_.measures.apply[Measure.Cluster].number).toSet.size}")
    res
  }

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def postProcess(result: Ruleset): Ruleset = {
    result.`export`(exportPath)
    result
  }
}
