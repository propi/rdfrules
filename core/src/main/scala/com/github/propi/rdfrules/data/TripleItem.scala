package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.BasicExtractors.AnyToDouble
import eu.easyminer.discretization.impl.{IntervalBound, Interval => DiscretizationInterval}
import org.apache.jena.datatypes.RDFDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.{Node, NodeFactory}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
sealed trait TripleItem {
  //def resolved(implicit mapper: TripleItemHashIndex): TripleItem = this
  def intern: TripleItem
}

object TripleItem {

  val sameAs: Uri = TripleItem.Uri("http://www.w3.org/2002/07/owl#sameAs")

  sealed trait Uri extends TripleItem {
    def hasSameUriAs(uri: Uri): Boolean

    def intern: Uri

    def uri: String
    //override def resolved(implicit mapper: TripleItemHashIndex): Uri = this
  }

  object Uri {
    implicit def apply(uri: String): Uri = LongUri(uri)
  }

  case class LongUri(uri: String) extends Uri {
    /*def toPrefixedUri: PrefixedUri = {
      val PrefixedUriPattern = "(.+[/#])(.+)".r
      uri match {
        case PrefixedUriPattern(nameSpace, localName) => PrefixedUri("", nameSpace, localName)
        case _ => throw new IllegalArgumentException
      }
    }*/

    /**
      * Explode URI to nameSpace and localName
      *
      * @return (String, String)
      */
    def explode: (String, String) = {
      val PrefixedUriPattern = "(.+[/#])(.+)".r
      uri match {
        case PrefixedUriPattern(nameSpace, localName) => nameSpace -> localName
        case _ => "" -> uri
      }
    }

    def intern: Uri = TripleItem.LongUri(uri.intern())

    def nameSpace: String = explode._1

    def localName: String = explode._2

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

  case class PrefixedUri(prefix: Prefix, localName: String) extends Uri {
    def toLongUri: LongUri = LongUri(uri)

    def uri: String = s"${prefix.nameSpace}$localName"

    def hasSameUriAs(uri: Uri): Boolean = toLongUri.hasSameUriAs(uri)

    /*override def resolved(implicit mapper: TripleItemHashIndex): Uri = if (nameSpace.isEmpty) {
      mapper.getNamespace(prefix).map(TripleItem.PrefixedUri(prefix, _, localName)).getOrElse(TripleItem.Uri(localName))
    } else {
      this
    }*/

    def intern: Uri = PrefixedUri(prefix, localName.intern())

    override def hashCode(): Int = toLongUri.hashCode()

    override def equals(obj: scala.Any): Boolean = obj match {
      case x: PrefixedUri => (this eq x) || hasSameUriAs(x)
      case x: Uri => hasSameUriAs(x)
      case _ => false
    }

    override def toString: String = prefix match {
      case Prefix.Full(prefix, _) =>
        if (prefix.isEmpty) {
          toLongUri.toString
        } else {
          s"$prefix:$localName"
        }
      case _: Prefix.Namespace => toLongUri.toString
    }
  }

  case class BlankNode(id: String) extends Uri {
    override def toString: String = s"_:$id"

    def uri: String = id

    def intern: Uri = BlankNode(id.intern())

    def hasSameUriAs(uri: Uri): Boolean = this == uri
  }

  sealed trait Literal extends TripleItem

  case class Text(value: String) extends Literal {
    def intern: TripleItem = Text(value.intern())

    override def toString: String = "\"" + value + "\""
  }

  case class Number[T](value: T)(implicit val n: Numeric[T]) extends Literal {
    def intern: TripleItem = this

    override def toString: String = value.toString
  }

  object NumberDouble {
    def unapply(arg: TripleItem): Option[Double] = arg match {
      case x@Number(_: Any) => Some(x.n.toDouble(x.value))
      case _ => None
    }
  }

  case class Interval(interval: DiscretizationInterval) extends Literal {
    def intern: TripleItem = this

    override def toString: String = s"${if (interval.isLeftBoundClosed()) "[" else "("} ${interval.getLeftBoundValue()} ; ${interval.getRightBoundValue()} ${if (interval.isRightBoundClosed()) "]" else ")"}"
  }

  object Interval {
    implicit def discretizationIntervalToInterval(interval: DiscretizationInterval): Interval = Interval(interval)

    def apply(text: String): Option[Interval] = {
      val trimmedText = if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
        text.substring(1, text.length - 1).trim
      } else {
        text
      }
      if ((trimmedText.startsWith("[") || trimmedText.startsWith("(")) && (trimmedText.endsWith("]") || trimmedText.endsWith(")"))) {
        val IntervalPattern = "(\\[|\\()\\s*(.+?)\\s*;\\s*(.+?)\\s*(\\]|\\))".r
        text match {
          case IntervalPattern(lb, AnyToDouble(lv), AnyToDouble(rv), rb) => Some(DiscretizationInterval(
            if (lb == "[") IntervalBound.Inclusive(lv) else IntervalBound.Exclusive(lv),
            if (rb == "]") IntervalBound.Inclusive(rv) else IntervalBound.Exclusive(rv)
          ))
          case _ => None
        }
      } else {
        None
      }
    }
  }

  case class BooleanValue(value: Boolean) extends Literal {
    def intern: TripleItem = this

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
    case PrefixedUri(prefix, localName) => NodeFactory.createURI(prefix.nameSpace + localName)
    case BlankNode(id) => NodeFactory.createBlankNode(id)
    case Text(value) => NodeFactory.createLiteral(value)
    case number: Number[_] => NodeFactory.createLiteral(number.toString, number: RDFDatatype)
    case boolean: BooleanValue => NodeFactory.createLiteral(boolean.toString, XSDDatatype.XSDboolean)
    case interval: Interval => NodeFactory.createLiteral(interval.toString)
  }

  implicit val tripleItemUriJsonFormat: RootJsonFormat[TripleItem.Uri] = new RootJsonFormat[TripleItem.Uri] {
    def write(obj: TripleItem.Uri): JsValue = obj match {
      case x: TripleItem.LongUri => x.toString.toJson
      case x: TripleItem.PrefixedUri => JsObject("prefix" -> x.prefix.prefix.toJson, "nameSpace" -> x.prefix.nameSpace.toJson, "localName" -> x.localName.toJson)
      case x: TripleItem.BlankNode => x.toString.toJson
    }

    def read(json: JsValue): TripleItem.Uri = {
      val LongUriPattern = "<(.*)>".r
      val BlankNodePattern = "_:(.+)".r
      json match {
        case JsString(LongUriPattern(uri)) => TripleItem.LongUri(uri)
        case JsString(BlankNodePattern(x)) => TripleItem.BlankNode(x)
        case JsObject(fields) if List("prefix", "nameSpace", "localName").forall(fields.contains) =>
          val shortPrefix = fields("prefix").convertTo[String]
          val prefix = if (shortPrefix.isEmpty) Prefix(fields("nameSpace").convertTo[String]) else Prefix(shortPrefix, fields("nameSpace").convertTo[String])
          TripleItem.PrefixedUri(prefix, fields("localName").convertTo[String])
        case x => deserializationError(s"Invalid triple item value: $x")
      }
    }
  }

  implicit val tripleItemJsonFormat: RootJsonFormat[TripleItem] = new RootJsonFormat[TripleItem] {
    def write(obj: TripleItem): JsValue = obj match {
      case TripleItem.NumberDouble(x) => JsNumber(x)
      case TripleItem.BooleanValue(x) => JsBoolean(x)
      case x: TripleItem.Uri => x.toJson
      case x: TripleItem => JsString(x.toString)
    }

    def read(json: JsValue): TripleItem = {
      val TextPattern = "\"(.*)\"".r
      val IntervalMatcher = new {
        def unapply(arg: String): Option[TripleItem.Interval] = TripleItem.Interval(arg)
      }
      json match {
        case JsNumber(x) => TripleItem.Number(x)
        case JsBoolean(x) => TripleItem.BooleanValue(x)
        case JsString(TextPattern(x)) => TripleItem.Text(x)
        case JsString(IntervalMatcher(x)) => x
        case x => x.convertTo[TripleItem.Uri]
      }
    }
  }

}
