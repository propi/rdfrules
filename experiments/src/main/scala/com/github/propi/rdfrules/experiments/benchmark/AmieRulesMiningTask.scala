package com.github.propi.rdfrules.experiments.benchmark

import java.util

import amie.mining.AMIE
import amie.rules.Rule
import com.github.propi.rdfrules.experiments.AmieRuleOps._
import com.github.propi.rdfrules.ruleset.ResolvedRule

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait AmieRulesMiningTask[I] extends Task[AMIE, util.List[Rule], IndexedSeq[ResolvedRule]] {

  protected def taskBody(input: AMIE): util.List[Rule] = input.mine()

  protected def postProcess(result: util.List[Rule]): IndexedSeq[ResolvedRule] = result.iterator().asScala.map(_.toResolvedRule).toIndexedSeq

}