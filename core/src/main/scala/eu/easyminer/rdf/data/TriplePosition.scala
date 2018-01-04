package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TriplePosition

object TriplePosition {

  case object Subject extends TriplePosition

  case object Predicate extends TriplePosition

  case object Object extends TriplePosition

}
