package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.data.Compression

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetSource

object RulesetSource {

  case object Text extends RulesetSource

  case object Json extends RulesetSource

  def apply(extension: String): RulesetSource = extension.toLowerCase match {
    case "txt" | "rules" => Text
    case "json" => Json
    case x => throw new IllegalArgumentException(s"Unsupported Ruleset format: $x")
  }

  implicit def rulesetSourceToRulesetReader(rulesetSource: RulesetSource): RulesetReader = rulesetSource match {
    case Text => Text
    case Json => Json
  }

  implicit def rulesetSourceToRulesetWriter(rulesetSource: RulesetSource): RulesetWriter = rulesetSource match {
    case Text => Text
    case Json => Json
  }

  case class CompressedRulesetSource(rulesetSource: RulesetSource, compression: Compression)

  implicit class PimpedRulesetSource(val rulesetSource: RulesetSource) extends AnyVal {
    def compressedBy(compression: Compression): CompressedRulesetSource = CompressedRulesetSource(rulesetSource, compression)
  }

}