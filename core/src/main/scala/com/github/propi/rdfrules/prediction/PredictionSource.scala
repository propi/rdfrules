package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.Compression

import scala.language.implicitConversions

import com.github.propi.rdfrules.prediction.formats.NDJson._

trait PredictionSource

object PredictionSource {

  case object NDJson extends PredictionSource

  def apply(extension: String): PredictionSource = extension.toLowerCase match {
    case "ndjson" => NDJson
    case x => throw new IllegalArgumentException(s"Unsupported Ruleset format: $x")
  }

  implicit def predictionSourceToPredictionReader(predictionSource: PredictionSource): PredictionReader = predictionSource match {
    case NDJson => NDJson
  }

  implicit def predictionSourceToPredictionWriter(predictionSource: PredictionSource): PredictionWriter = predictionSource match {
    case NDJson => NDJson
  }

  case class CompressedPredictionSource(predictionSource: PredictionSource, compression: Compression)

  implicit class PimpedPredictionSource(val predictionSource: PredictionSource) extends AnyVal {
    def compressedBy(compression: Compression): CompressedPredictionSource = CompressedPredictionSource(predictionSource, compression)
  }

}