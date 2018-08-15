package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.BasicExtractors.AnyToDouble
import eu.easyminer.discretization.impl.{IntervalBound, Interval => DiscretizationInterval}
import org.apache.jena.datatypes.RDFDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.{Node, NodeFactory}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
sealed trait TripleItem

object TripleItem {

  sealed trait Uri extends TripleItem {
    def hasSameUriAs(uri: Uri): Boolean
  }

  object Uri {
    implicit def apply(uri: String): Uri = LongUri(uri)
  }

  case class LongUri(uri: String) extends Uri {
    def toPrefixedUri: PrefixedUri = {
      val PrefixedUriPattern = "(.+/)(.+)".r
      uri match {
        case PrefixedUriPattern(nameSpace, localName) => PrefixedUri("", nameSpace, localName)
        case _ => throw new IllegalArgumentException
      }
    }

    def hasSameUriAs(uri: Uri): Boolean = uri match {
      case LongUri(uri) => uri == this.uri
      case x: PrefixedUri => hasSameUriAs(x.toLongUri)
      case _ => false
    }

    override def equals(obj: scala.Any): Boolean = obj match {
      case x: LongUri => (this eq x) || uri == x.uri
      case x: Uri => hasSameUriAs(x)
      case _ => false
    }

    override def toString: String = s"<$uri>"
  }

  case class PrefixedUri(prefix: String, nameSpace: String, localName: String) extends Uri {
    def toLongUri: LongUri = LongUri(nameSpace + localName)

    def hasSameUriAs(uri: Uri): Boolean = toLongUri.hasSameUriAs(uri)

    def toPrefix: Prefix = Prefix(prefix, nameSpace)

    override def hashCode(): Int = toLongUri.hashCode()

    override def equals(obj: scala.Any): Boolean = obj match {
      case x: PrefixedUri => (this eq x) || hasSameUriAs(x)
      case x: Uri => hasSameUriAs(x)
      case _ => false
    }

    override def toString: String = prefix + ":" + localName
  }

  case class BlankNode(id: String) extends Uri {
    override def toString: String = "_:" + id

    def hasSameUriAs(uri: Uri): Boolean = this == uri
  }

  sealed trait Literal extends TripleItem

  case class Text(value: String) extends Literal {
    override def toString: String = "\"" + value + "\""
  }

  case class Number[T](value: T)(implicit val n: Numeric[T]) extends Literal {
    override def toString: String = value.toString
  }

  object NumberDouble {
    def unapply(arg: TripleItem): Option[Double] = arg match {
      case x@Number(_: Any) => Some(x.n.toDouble(x.value))
      case _ => None
    }
  }

  case class Interval(interval: DiscretizationInterval) extends Literal {
    override def toString: String = s"${if (interval.isLeftBoundClosed()) "[" else "("} ${interval.getLeftBoundValue()} ; ${interval.getRightBoundValue()} ${if (interval.isRightBoundClosed()) "]" else ")"}"
  }

  object Interval {
    implicit def discretizationIntervalToInterval(interval: DiscretizationInterval): Interval = Interval(interval)

    def apply(text: String): Option[Interval] = {
      val IntervalPattern = "\"?\\s*(\\[|\\()\\s*(.+?)\\s*;\\s*(.+?)\\s*(\\]|\\))\\s*\"?".r
      text match {
        case IntervalPattern(lb, AnyToDouble(lv), AnyToDouble(rv), rb) => Some(DiscretizationInterval(
          if (lb == "[") IntervalBound.Inclusive(lv) else IntervalBound.Exclusive(lv),
          if (rb == "]") IntervalBound.Inclusive(rv) else IntervalBound.Exclusive(rv)
        ))
        case _ => None
      }
    }
  }

  case class BooleanValue(value: Boolean) extends Literal {
    override def toString: String = if (value) "true" else "false"
  }

  implicit def numberToRdfDatatype(number: Number[_]): RDFDatatype = number match {
    case Number(_: Int) => XSDDatatype.XSDint
    case Number(_: Double) => XSDDatatype.XSDdouble
    case Number(_: Float) => XSDDatatype.XSDfloat
    case Number(_: Long) => XSDDatatype.XSDlong
    case Number(_: Short) => XSDDatatype.XSDshort
    case Number(_: Byte) => XSDDatatype.XSDbyte
    case _ => throw new IllegalArgumentException
  }

  implicit def tripleItemToJenaNode(tripleItem: TripleItem): Node = tripleItem match {
    case LongUri(uri) => NodeFactory.createURI(uri)
    case PrefixedUri(_, nameSpace, localName) => NodeFactory.createURI(nameSpace + localName)
    case BlankNode(id) => NodeFactory.createBlankNode(id)
    case Text(value) => NodeFactory.createLiteral(value)
    case number: Number[_] => NodeFactory.createLiteral(number.toString, number: RDFDatatype)
    case boolean: BooleanValue => NodeFactory.createLiteral(boolean.toString, XSDDatatype.XSDboolean)
    case interval: Interval => NodeFactory.createLiteral(interval.toString)
  }

}
