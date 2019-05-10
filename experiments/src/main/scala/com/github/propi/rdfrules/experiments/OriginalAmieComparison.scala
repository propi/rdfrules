package com.github.propi.rdfrules.experiments

import amie.mining.AMIE
import amie.rules.Rule
import com.github.propi.rdfrules.algorithm.amie.{Amie, AtomCounting, RuleCounting}
import com.github.propi.rdfrules.data.{Graph, TripleItem}
import com.github.propi.rdfrules.rule.{AtomPattern, Measure, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.{Debugger, HowLong, Stringifier}
import AmieRuleOps._

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object OriginalAmieComparison {

  private def testAmie(): Unit = {
    println("*******************")
    println("AMIE")
    println("*******************")
    //-minpca 0.1 -minc 0.1 -oute
    val cmd = "-oute -maxad 4 -minhc 0.01 -minpca 0.1 -minc 0.1 experiments/data/yagoFacts.tsv"
    val amie = HowLong.howLong("Original AMIE loading", memUsage = true, forceShow = true) {
      AMIE.getInstance(cmd.split(' '))
    }
    val rules = HowLong.howLong("Original AMIE mining", memUsage = true, forceShow = true) {
      amie.mine()
    }.asScala.map(_.toResolvedRule)
    rules.sortBy(_.measures).foreach(println)
    println("mining rules: " + rules.size)
  }

  private def testRdfRules(): Unit = {
    println("*******************")
    println("RDFRules")
    println("*******************")

    val index = Graph("experiments/data/yagoFacts.tsv").index().withEvaluatedLazyVals
    HowLong.howLong("RDFRules loading", memUsage = true, forceShow = true) {
      index.tripleMap(x => println("loaded triples: " + x.size))
    }
    Debugger() { implicit debugger =>
      val rules = HowLong.howLong("RDFRules mining", memUsage = true, forceShow = true) {
        index
          .mine(Amie().addThreshold(Threshold.MinHeadCoverage(0.1)).addThreshold(Threshold.MaxRuleLength(3)).addConstraint(RuleConstraint.WithInstances(false)))//.addConstraint(RuleConstraint.WithInstances(false)))//.addPattern(AtomPattern(predicate = TripleItem.Uri("participatedIn"), `object` = TripleItem.Uri("Unified_Task_Force")) &: AtomPattern(predicate = TripleItem.Uri("hasGender")) =>: AtomPattern(predicate = TripleItem.Uri("isCitizenOf"))))
          //.computePcaConfidence(0.1)
          //.computeConfidence(0.1)
          //.computePcaConfidence(0.01)
          .cache
      }.resolvedRules.toIndexedSeq
      rules.sortBy(_.measures).foreach(println)
      println("mining rules: " + rules.size)
    }
  }

  def main(args: Array[String]): Unit = {
    //val cmd = "-const -minhc 0.01 -htr <participatedIn> yago2core.10kseedsSample.compressed.notypes.tsv"
    //val cmd = "-const -minhc 0.01 -htr <participatedIn> -nc 1 yago2core.10kseedsSample.compressed.notypes.tsv"
    //testAmie()
    testRdfRules()
    //testRdfRules()
    HowLong.flushAllResults()
    //testRdfRules()
  }

}
