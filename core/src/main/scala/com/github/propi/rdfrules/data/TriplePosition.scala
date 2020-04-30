package com.github.propi.rdfrules.data

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TriplePosition

object TriplePosition {

  sealed trait ConceptPosition extends TriplePosition

  case object Subject extends ConceptPosition

  case object Predicate extends TriplePosition

  case object Object extends ConceptPosition

}
