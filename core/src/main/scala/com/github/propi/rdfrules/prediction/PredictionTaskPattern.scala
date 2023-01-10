package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition

case class PredictionTaskPattern(p: Int, targetVariable: ConceptPosition) {
  def predictorVariable: ConceptPosition = targetVariable match {
    case TriplePosition.Subject => TriplePosition.Object
    case TriplePosition.Object => TriplePosition.Subject
  }
}