package com.github.propi.rdfrules.data

import spray.json._
import DefaultJsonProtocol._

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TriplePosition

object TriplePosition {
  sealed trait ConceptPosition extends TriplePosition with Product

  case object Subject extends ConceptPosition

  case object Predicate extends TriplePosition

  case object Object extends ConceptPosition

  implicit val conceptPositionWriter: JsonWriter[ConceptPosition] = (obj: ConceptPosition) => obj.productPrefix.toJson
}
