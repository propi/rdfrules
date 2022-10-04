package com.github.propi.rdfrules.experiments.benchmark

import java.util

import amie.mining.AMIE
import amie.rules.Rule
import com.github.propi.rdfrules.experiments.AmieRuleOps._
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.HowLong

import scala.jdk.CollectionConverters._

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait AmieRulesMiningTask extends Task[String, AMIE, util.List[Rule], IndexedSeq[ResolvedRule]] with TaskPreProcessor[String, AMIE] with TaskPostProcessor[util.List[Rule], IndexedSeq[ResolvedRule]] with DefaultMiningSettings {

  val confidenceCountingHeuristicOn = false

  // -optimfh
  protected def preProcess(input: String): AMIE = {
    val cmd = List(
      s"-oute -dpr -maxad $maxRuleLength -minhc $minHeadCoverage -minpca $minPcaConfidence -minc $minConfidence -nc $numberOfThreads",
      if (allowConstants) " -const" else "",
      if (confidenceCountingHeuristicOn) " -optimcb -optimfh" else "",
      " " + input
    ).mkString
    HowLong.howLong("Original AMIE+ loading", memUsage = true, forceShow = true) {
      AMIE.getInstance(cmd.split(' '))
    }
  }

  final protected def taskBody(input: AMIE): util.List[Rule] = input.mine()

  final protected def postProcess(result: util.List[Rule]): IndexedSeq[ResolvedRule] = result.iterator().asScala.map(_.toResolvedRule).toIndexedSeq

}