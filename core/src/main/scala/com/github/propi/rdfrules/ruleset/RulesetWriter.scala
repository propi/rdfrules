package com.github.propi.rdfrules.ruleset

import java.io.File
import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.data.formats.Compressed
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.{ForEach, OutputStreamBuilder}

import scala.util.Try

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetWriter {
  def writeToOutputStream(rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(ruleset: Ruleset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(ruleset.resolvedRules, outputStreamBuilder)
}

object RulesetWriter extends Compressed {

  implicit object NoWriter extends RulesetWriter {
    def writeToOutputStream(rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder): Unit = throw new IllegalStateException("No specified RulesetWriter.")
  }

  def apply(file: File): RulesetWriter = {
    val Ext2 = ".+[.](.+)[.](.+)".r
    val Ext1 = ".+[.](.+)".r
    file.getName.toLowerCase match {
      case Ext2(ext1, ext2) => Try(Compression(ext2)).toOption match {
        case Some(compression) => RulesetSource(ext1).compressedBy(compression)
        case None => RulesetSource(ext2)
      }
      case Ext1(ext1) => RulesetSource(ext1)
      case _ => throw new IllegalArgumentException("No file extension to detect a Ruleset format.")
    }
  }

}