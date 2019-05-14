package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset}

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait RdfRulesMiningTask[I] extends Task[RulesMining, Ruleset, IndexedSeq[ResolvedRule]] {

  val index: Index

  protected def transformRuleset(ruleset: Ruleset): Ruleset = ruleset

  protected def taskBody(input: RulesMining): Ruleset = transformRuleset(index.mine(input))

  protected def postProcess(result: Ruleset): IndexedSeq[ResolvedRule] = result.resolvedRules.toIndexedSeq
}