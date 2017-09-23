package eu.easyminer.rdf.data

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
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