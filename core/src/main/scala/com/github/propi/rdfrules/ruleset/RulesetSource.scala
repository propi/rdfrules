package com.github.propi.rdfrules.ruleset

import java.io.File

import com.github.propi.rdfrules.algorithm.consumer.PrettyPrintedWriter
import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.utils.Stringifier
import spray.json.DeserializationException

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetSource

object RulesetSource {

  case object Text extends RulesetSource

  case object Json extends RulesetSource

  def apply(extension: String): RulesetSource = extension.toLowerCase match {
    case "txt" => Text
    case "json" | "rules" => Json
    case x => throw new IllegalArgumentException(s"Unsupported Ruleset format: $x")
  }

  implicit def rulesetSourceToRulesetReader(rulesetSource: RulesetSource): RulesetReader = rulesetSource match {
    case Text => throw DeserializationException("The 'text' rules format is not parseable. Use the JSON rules format.")
    case Json => Json
  }

  implicit def rulesetSourceToRulesetWriter(rulesetSource: RulesetSource): RulesetWriter = rulesetSource match {
    case Text => Text
    case Json => Json
  }

  implicit def rulesetSourceToPrettyPrintedWriterBuilder(rulesetSource: RulesetSource)(implicit mapper: TripleItemIndex, stringifier: Stringifier[ResolvedRule]): File => PrettyPrintedWriter = rulesetSource match {
    case Text => Text
    case Json => Json
  }

  case class CompressedRulesetSource(rulesetSource: RulesetSource, compression: Compression)

  implicit class PimpedRulesetSource(val rulesetSource: RulesetSource) extends AnyVal {
    def compressedBy(compression: Compression): CompressedRulesetSource = CompressedRulesetSource(rulesetSource, compression)
  }

}