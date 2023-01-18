package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.experiments.AnyBurlRulesetFormat
import com.github.propi.rdfrules.rule.ResolvedRule
import de.unima.ki.anyburl

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
object AnyBurlMiningTask extends Task[AnyBurlSettings, AnyBurlSettings, AnyBurlSettings, IndexedSeq[ResolvedRule]] with TaskPreProcessor[AnyBurlSettings, AnyBurlSettings] with TaskPostProcessor[AnyBurlSettings, IndexedSeq[ResolvedRule]] {
  val name: String = "AnyBurl mining"

  protected def preProcess(input: AnyBurlSettings): AnyBurlSettings = input

  protected def taskBody(input: AnyBurlSettings): AnyBurlSettings = {
    anyburl.Learn.main(Array(input.saveConfigTo("experiments/data/config-learn.properties").getAbsolutePath))
    input
  }

  protected def postProcess(result: AnyBurlSettings): IndexedSeq[ResolvedRule] = AnyBurlRulesetFormat.readRules(result.rulesFile.getAbsolutePath).toIndexedSeq
}