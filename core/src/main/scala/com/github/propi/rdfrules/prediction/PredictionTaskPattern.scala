package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.TripleItemIndex

case class PredictionTaskPattern(p: Option[TripleItem.Uri], targetVariable: Option[ConceptPosition]) {
  def mapped(implicit tripleItemIndex: TripleItemIndex): PredictionTaskPattern.Mapped = PredictionTaskPattern.Mapped(p.map(tripleItemIndex.getIndex), targetVariable)
}

object PredictionTaskPattern {
  case class Mapped(p: Option[Int], targetVariable: Option[ConceptPosition]) {
    def predictorVariable: Option[ConceptPosition] = targetVariable.map {
      case TriplePosition.Subject => TriplePosition.Object
      case TriplePosition.Object => TriplePosition.Subject
    }

    def matchPreditionTask(predictionTask: PredictionTask): Boolean = p.forall(_ == predictionTask.p) && targetVariable.forall(_ == predictionTask.targetVariable)
  }
}