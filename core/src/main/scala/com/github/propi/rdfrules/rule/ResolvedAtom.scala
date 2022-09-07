package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.language.implicitConversions

sealed trait ResolvedAtom {
  def subject: ResolvedItem

  def predicate: TripleItem.Uri

  def `object`: ResolvedItem

  def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom

  def toAtomOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom]

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: ResolvedAtom => (this eq x) || (subject == x.subject && predicate == x.predicate && `object` == x.`object`)
    case _ => false
  }
}

object ResolvedAtom {

  sealed trait GraphAware extends ResolvedAtom {
    def graphs: Set[TripleItem.Uri]
  }

  sealed trait ResolvedItem {
    def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Item

    def toItemOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom.Item]
  }

  object ResolvedItem {

    case class Variable private(value: String) extends ResolvedItem {
      def toVariable: Atom.Variable = Atom.Item(value)

      def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Variable = toVariable

      def toItemOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom.Item] = Some(toVariable)
    }

    case class Constant private(tripleItem: TripleItem) extends ResolvedItem {
      def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Constant = Atom.Item(tripleItem)

      def toItemOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom.Item] = tripleItemIndex.getIndexOpt(tripleItem).map(Atom.Constant)
    }

    def apply(char: Char): ResolvedItem = Variable("?" + char)

    def apply(variable: String): ResolvedItem = Variable(variable)

    def apply(tripleItem: TripleItem): ResolvedItem = Constant(tripleItem)

    implicit def apply(atomItem: rule.Atom.Item)(implicit mapper: TripleItemIndex): ResolvedItem = atomItem match {
      case x: rule.Atom.Variable => apply(x.value)
      case rule.Atom.Constant(x) => apply(mapper.getTripleItem(x))
    }

    implicit val mappedAtomItemJsonFormat: RootJsonFormat[ResolvedItem] = new RootJsonFormat[ResolvedItem] {
      def write(obj: ResolvedItem): JsValue = obj match {
        case ResolvedItem.Variable(x) => JsObject("type" -> JsString("variable"), "value" -> JsString(x))
        case ResolvedItem.Constant(x) => JsObject("type" -> JsString("constant"), "value" -> x.toJson)
      }

      def read(json: JsValue): ResolvedItem = {
        val fields = json.asJsObject.fields
        val value = fields("value")
        fields("type").convertTo[String] match {
          case "variable" => ResolvedItem.Variable(value.convertTo[String])
          case "constant" => ResolvedItem.Constant(value.convertTo[TripleItem])
          case x => deserializationError(s"Invalid triple item type: $x")
        }
      }
    }
  }

  private case class Basic private(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem) extends ResolvedAtom {
    def toAtomOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom] = for {
      s <- subject.toItemOpt
      p <- tripleItemIndex.getIndexOpt(predicate)
      o <- `object`.toItemOpt
    } yield {
      rule.Atom(s, p, o)
    }

    def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom = rule.Atom(subject.toItem, tripleItemIndex.getIndex(predicate), `object`.toItem)
  }

  private case class GraphAwareBasic private(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem)(val graphs: Set[TripleItem.Uri]) extends GraphAware {
    def toAtomOpt(implicit tripleItemIndex: TripleItemIndex): Option[Atom] = for {
      s <- subject.toItemOpt
      p <- tripleItemIndex.getIndexOpt(predicate)
      o <- `object`.toItemOpt
    } yield {
      rule.Atom(s, p, o, graphs.flatMap(tripleItemIndex.getIndexOpt))
    }

    def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom = rule.Atom(subject.toItem, tripleItemIndex.getIndex(predicate), `object`.toItem, graphs.map(tripleItemIndex.getIndex))
  }

  def apply(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem): ResolvedAtom = Basic(subject, predicate, `object`)

  def apply(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem, graphs: Set[TripleItem.Uri]): GraphAware = GraphAwareBasic(subject, predicate, `object`)(graphs)

  implicit def apply(atom: rule.Atom)(implicit mapper: TripleItemIndex): ResolvedAtom = atom match {
    case x: rule.Atom.GraphAware =>
      GraphAwareBasic(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)(x.graphsIterator.map(mapper.getTripleItem(_).asInstanceOf[TripleItem.Uri]).toSet)
    case _ =>
      apply(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)
  }

  implicit val mappedAtomJsonFormat: RootJsonFormat[ResolvedAtom] = new RootJsonFormat[ResolvedAtom] {
    def write(obj: ResolvedAtom): JsValue = obj match {
      case v: ResolvedAtom.GraphAware => JsObject(
        "subject" -> v.subject.toJson,
        "predicate" -> v.predicate.toJson,
        "object" -> v.`object`.toJson,
        "graphs" -> v.graphs.map(_.toJson).toJson
      )
      case _ => JsObject(
        "subject" -> obj.subject.toJson,
        "predicate" -> obj.predicate.toJson,
        "object" -> obj.`object`.toJson
      )
    }

    def read(json: JsValue): ResolvedAtom = {
      val fields = json.asJsObject.fields
      val s = fields("subject").convertTo[ResolvedItem]
      val p = fields("predicate").convertTo[TripleItem.Uri]
      val o = fields("object").convertTo[ResolvedItem]
      fields.get("graphs").map(_.convertTo[Set[TripleItem.Uri]]).map(ResolvedAtom(s, p, o, _)).getOrElse(ResolvedAtom(s, p, o))
    }
  }

}