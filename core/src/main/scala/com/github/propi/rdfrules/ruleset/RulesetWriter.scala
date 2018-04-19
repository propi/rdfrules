package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.utils.OutputStreamBuilder

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetWriter[+T <: RulesetSource] {
  def writeToOutputStream(rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(ruleset: Ruleset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(ruleset.resolvedRules, outputStreamBuilder)
}
