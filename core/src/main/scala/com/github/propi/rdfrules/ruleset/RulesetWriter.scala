package com.github.propi.rdfrules.ruleset

import java.io.File

import com.github.propi.rdfrules.utils.OutputStreamBuilder

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetWriter {
  def writeToOutputStream(rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(ruleset: Ruleset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(ruleset.resolvedRules, outputStreamBuilder)
}

object RulesetWriter {

  implicit object NoWriter extends RulesetWriter {
    def writeToOutputStream(rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder): Unit = throw new IllegalStateException("No specified RulesetWriter.")
  }

  def apply(file: File): RulesetWriter = file.getName.replaceAll(".*\\.", "").toLowerCase match {
    case "txt" => RulesetSource.Text
    case "json" => RulesetSource.Json
    case x => throw new IllegalArgumentException(s"Unsupported Ruleset extension: $x")
  }

}