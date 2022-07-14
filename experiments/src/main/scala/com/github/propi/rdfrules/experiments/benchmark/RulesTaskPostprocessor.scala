package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 21. 5. 2019.
  */
trait RulesTaskPostprocessor extends TaskPostProcessor[Ruleset, IndexedSeq[ResolvedRule]] {

  protected def postProcess(result: Ruleset): IndexedSeq[ResolvedRule] = result.resolvedRules.toIndexedSeq

}
