package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.consumer.RuleIO

import java.io.File
import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.Stringifier
import spray.json.DeserializationException
import com.github.propi.rdfrules.ruleset.formats.Json._
import com.github.propi.rdfrules.ruleset.formats.Text._
import com.github.propi.rdfrules.ruleset.formats.NDJson._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetSource

object RulesetSource {

  case object Text extends RulesetSource

  case object Json extends RulesetSource

  case object NDJson extends RulesetSource

  def apply(extension: String): RulesetSource = extension.toLowerCase match {
    case "txt" => Text
    case "json" | "rules" => Json
    case "ndjson" => NDJson
    case x => throw new IllegalArgumentException(s"Unsupported Ruleset format: $x")
  }

  implicit def rulesetSourceToRulesetReader(rulesetSource: RulesetSource): RulesetReader = rulesetSource match {
    case Text => throw DeserializationException("The 'text' rules format is not parseable. Use the JSON rules format.")
    case Json => Json
    case NDJson => NDJson
  }

  implicit def rulesetSourceToRulesetWriter(rulesetSource: RulesetSource): RulesetWriter = rulesetSource match {
    case Text => Text
    case Json => Json
    case NDJson => NDJson
  }

  implicit def rulesetSourceToRuleIOBuilder(rulesetSource: RulesetSource)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]): File => RuleIO = rulesetSource match {
    case Text => Text
    case NDJson => NDJson
    case x => throw new IllegalArgumentException(s"Unsupported RuleIO format: $x")
  }

  case class CompressedRulesetSource(rulesetSource: RulesetSource, compression: Compression)

  implicit class PimpedRulesetSource(val rulesetSource: RulesetSource) extends AnyVal {
    def compressedBy(compression: Compression): CompressedRulesetSource = CompressedRulesetSource(rulesetSource, compression)
  }

}