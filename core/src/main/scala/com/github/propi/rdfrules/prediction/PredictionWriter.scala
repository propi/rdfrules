package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.Compression
import com.github.propi.rdfrules.data.formats.Compressed
import com.github.propi.rdfrules.utils.{ForEach, OutputStreamBuilder}

import java.io.File
import scala.util.Try

trait PredictionWriter {
  def writeToOutputStream(rules: ForEach[ResolvedPredictedTriple], outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(prediction: PredictedTriples, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(prediction.resolvedTriples, outputStreamBuilder)
}

object PredictionWriter extends Compressed {

  implicit object NoWriter extends PredictionWriter {
    def writeToOutputStream(triples: ForEach[ResolvedPredictedTriple], outputStreamBuilder: OutputStreamBuilder): Unit = throw new IllegalStateException("No specified PredictionWriter.")
  }

  def apply(file: File): PredictionWriter = {
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