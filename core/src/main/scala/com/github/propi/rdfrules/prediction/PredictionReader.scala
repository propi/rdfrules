package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.data.formats.Compressed
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder}

import java.io.{File, FileInputStream}
import scala.util.Try

trait PredictionReader {
  def fromInputStream(inputStreamBuilder: InputStreamBuilder): ForEach[ResolvedPredictedTriple]

  def fromFile(file: File): ForEach[ResolvedPredictedTriple] = fromInputStream(new FileInputStream(file))
}

object PredictionReader extends Compressed {

  implicit object NoReader extends PredictionReader {
    def fromInputStream(inputStreamBuilder: InputStreamBuilder): ForEach[ResolvedPredictedTriple] = throw new IllegalStateException("No specified RulesetReader.")
  }

  def apply(file: File): PredictionReader = {
    val Ext2 = ".+[.](.+)[.](.+)".r
    val Ext1 = ".+[.](.+)".r
    file.getName.toLowerCase match {
      case Ext2(ext1, ext2) => Try(Compression(ext2)).toOption match {
        case Some(compression) => PredictionSource(ext1).compressedBy(compression)
        case None => PredictionSource(ext2)
      }
      case Ext1(ext1) => PredictionSource(ext1)
      case _ => throw new IllegalArgumentException("No file extension to detect a Ruleset format.")
    }
  }

}