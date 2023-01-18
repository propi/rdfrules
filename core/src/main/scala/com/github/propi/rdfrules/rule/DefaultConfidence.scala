package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap

sealed trait DefaultConfidence {
  def confidenceType: Option[Measure.Confidence[Measure.ConfidenceMeasure]]

  def typedConfidence(measures: TypedKeyMap.Immutable[Measure]): Option[Measure.ConfidenceMeasure]

  final def confidenceOpt(measures: TypedKeyMap.Immutable[Measure]): Option[Double] = typedConfidence(measures).map(_.value)

  def isDefined(measures: TypedKeyMap.Immutable[Measure]): Boolean

  final def confidence(measures: TypedKeyMap.Immutable[Measure]): Double = confidenceOpt(measures).getOrElse(0.0)
}

object DefaultConfidence {
  private class Selected(confidence: Measure.Confidence[Measure.ConfidenceMeasure]) extends DefaultConfidence {
    def typedConfidence(measures: TypedKeyMap.Immutable[Measure]): Option[Measure.ConfidenceMeasure] = measures.get(confidence)

    def confidenceType: Option[Measure.Confidence[Measure.ConfidenceMeasure]] = Some(confidence)

    override def isDefined(measures: TypedKeyMap.Immutable[Measure]): Boolean = measures.exists(confidence)
  }

  private object Auto extends DefaultConfidence {
    def typedConfidence(measures: TypedKeyMap.Immutable[Measure]): Option[Measure.ConfidenceMeasure] = measures.get(Measure.QpcaConfidence)
      .orElse(measures.get(Measure.PcaConfidence))
      .orElse(measures.get(Measure.CwaConfidence))

    def confidenceType: Option[Measure.Confidence[Measure.ConfidenceMeasure]] = None

    def isDefined(measures: TypedKeyMap.Immutable[Measure]): Boolean = measures.exists(Measure.QpcaConfidence) || measures.exists(Measure.PcaConfidence) || measures.exists(Measure.CwaConfidence)
  }

  def apply(confidence: Measure.Confidence[Measure.ConfidenceMeasure]): DefaultConfidence = new Selected(confidence)

  def apply(): DefaultConfidence = Auto
}