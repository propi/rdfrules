package com.github.propi.rdfrules.experiments

import amie.mining.AMIE
import amie.rules.Rule
import com.github.propi.rdfrules.algorithm.amie.{Amie, AtomCounting, RuleCounting, RuleRefinement}
import com.github.propi.rdfrules.data.{Graph, TripleItem}
import com.github.propi.rdfrules.rule.{AtomPattern, Measure, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset}
import com.github.propi.rdfrules.utils.{Debugger, HowLong, Stringifier}
import AmieRuleOps._
import com.github.propi.rdfrules.rule

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object OriginalAmieComparison {

  private def testAmie(): Unit = {
    println("*******************")
    println("AMIE")
    println("*******************")
    //-minpca 0.1 -minc 0.1 -oute -const -htr <dealsWith>
    val cmd = "-oute -maxad 3 -minhc 0.01 -minpca 0.1 -minc 0.1 experiments/data/mappingbased_objects_sample.tsv"
    val amie = HowLong.howLong("Original AMIE loading", memUsage = true, forceShow = true) {
      AMIE.getInstance(cmd.split(' '))
    }
    val rules = HowLong.howLong("Original AMIE mining", memUsage = true, forceShow = true) {
      amie.mine()
    }.asScala.map(_.toResolvedRule)
    //rules.sortBy(_.measures).foreach(println)
    println("mining rules: " + rules.size)
  }

  private def testRdfRules(): Unit = {
    println("*******************")
    println("RDFRules")
    println("*******************")

    Debugger() { implicit debugger =>
      val index = Graph("experiments/data/mappingbased_objects_sample.tsv").index().withEvaluatedLazyVals
      HowLong.howLong("RDFRules loading", memUsage = true, forceShow = true) {
        index.tripleMap(x => println("loaded triples: " + x.size))
      }
      val rules = HowLong.howLong("RDFRules mining", memUsage = true, forceShow = true) {
        index
          .mine(Amie().addThreshold(Threshold.MinHeadCoverage(0.01)).addThreshold(Threshold.MaxRuleLength(3)))//.addPattern(AtomPattern(predicate = TripleItem.Uri("http://dbpedia.org/ontology/child")) =>: Option.empty[AtomPattern]))
          .computePcaConfidence(0.1)
          .computeConfidence(0.1)
          //.computePcaConfidence(0.01)
          .cache
      }.resolvedRules.toIndexedSeq
      /*val rules = HowLong.howLong("RDFRules mining", memUsage = true, forceShow = true) {
        Ruleset.fromCache(index, "testrules.cache").computePcaConfidence(0.1, 100).resolvedRules.toIndexedSeq
      }*/
      /*index.tripleItemMap { implicit mapper =>
        RuleRefinement.badRules.iterator.map(rule.Rule.Simple(_)).map(ResolvedRule.apply(_)).foreach(println)
      }*/

      //rules.sortBy(_.measures).foreach(println)
      println("mining rules: " + rules.size)
    }
  }

  def main(args: Array[String]): Unit = {
    //val cmd = "-const -minhc 0.01 -htr <participatedIn> yago2core.10kseedsSample.compressed.notypes.tsv"
    //val cmd = "-const -minhc 0.01 -htr <participatedIn> -nc 1 yago2core.10kseedsSample.compressed.notypes.tsv"
    //
    testAmie()
    testRdfRules()
    //testRdfRules()
    HowLong.flushAllResults()
    //testRdfRules()
  }

}
