package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting.BodyAtomsSimilarityCounting
import com.github.propi.rdfrules.algorithm.clustering.{SimilarityCounting, TreeBasedDbScan}
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

class ClusteringTask(val name: String, exportPath: String)(implicit debugger: Debugger) extends Task[Ruleset, Ruleset, Ruleset, Ruleset] with TaskPreProcessor[Ruleset, Ruleset] with TaskPostProcessor[Ruleset, Ruleset] {
  protected def taskBody(input: Ruleset): Ruleset = {
    implicit val similarityCounting: SimilarityCounting[FinalRule] = BodyAtomsSimilarityCounting
    input.makeClusters(TreeBasedDbScan()).cache
  }

  protected def preProcess(input: Ruleset): Ruleset = input

  protected def postProcess(result: Ruleset): Ruleset = {
    result.`export`(exportPath)
    result
  }
}
