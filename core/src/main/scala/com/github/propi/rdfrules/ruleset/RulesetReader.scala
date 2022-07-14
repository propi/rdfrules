package com.github.propi.rdfrules.ruleset

import java.io.{File, FileInputStream}
import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.data.formats.Compressed
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder}

import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RulesetReader {
  def fromInputStream(inputStreamBuilder: InputStreamBuilder): ForEach[ResolvedRule]

  def fromFile(file: File): ForEach[ResolvedRule] = fromInputStream(new FileInputStream(file))
}

object RulesetReader extends Compressed {

  implicit object NoReader extends RulesetReader {
    def fromInputStream(inputStreamBuilder: InputStreamBuilder): ForEach[ResolvedRule] = throw new IllegalStateException("No specified RulesetReader.")
  }

  def apply(file: File): RulesetReader = {
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