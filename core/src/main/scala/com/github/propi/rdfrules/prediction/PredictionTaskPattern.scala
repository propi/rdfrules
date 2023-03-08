package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.TripleItemIndex

case class PredictionTaskPattern(p: TripleItem.Uri, targetVariable: ConceptPosition) {
  def mapped(implicit tripleItemIndex: TripleItemIndex): PredictionTaskPattern.Mapped = PredictionTaskPattern.Mapped(tripleItemIndex.getIndex(p), targetVariable)
}

object PredictionTaskPattern {
  case class Mapped(p: Int, targetVariable: ConceptPosition) {
    def predictorVariable: ConceptPosition = targetVariable match {
      case TriplePosition.Subject => TriplePosition.Object
      case TriplePosition.Object => TriplePosition.Subject
    }
  }
}