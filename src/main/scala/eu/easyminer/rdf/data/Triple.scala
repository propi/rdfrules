package eu.easyminer.rdf.data

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: String, predicate: String, `object`: TripleObject)

case class CompressedTriple(subject: Int, predicate: Int, `object`: Int)

sealed trait TripleObject {
  def toStringValue: String = this
}

object TripleObject {

  case class NumericLiteral[T](value: T)(implicit val n: Numeric[T]) extends TripleObject

  case class NominalLiteral(value: String) extends TripleObject

  case class Uri(value: String) extends TripleObject

  implicit def tripleObjectToString(tripleObject: TripleObject): String = tripleObject match {
    case NumericLiteral(value) => value.toString
    case NominalLiteral(value) => value
    case Uri(value) => value
  }

}