package com.github.propi.rdfrules.ruleset.formats

import java.io.{BufferedInputStream, OutputStreamWriter, PrintWriter}

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.ResolvedRule.Atom
import com.github.propi.rdfrules.ruleset.{ResolvedRule, RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder, TypedKeyMap}
import spray.json._
import DefaultJsonProtocol._

import scala.io.Source
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait Json {

  private implicit val tripleItemUriJsonFormat: RootJsonFormat[TripleItem.Uri] = new RootJsonFormat[TripleItem.Uri] {
    def write(obj: TripleItem.Uri): JsValue = obj match {
      case x: TripleItem.LongUri => x.toString.toJson
      case x: TripleItem.PrefixedUri => write(x.toLongUri)
      case x: TripleItem.BlankNode => x.toString.toJson
    }

    def read(json: JsValue): TripleItem.Uri = {
      val LongUriPattern = "<(.*)>".r
      val BlankNodePattern = "_:(.+)".r
      json.convertTo[String] match {
        case LongUriPattern(uri) => TripleItem.LongUri(uri)
        case BlankNodePattern(x) => TripleItem.BlankNode(x)
        case x => deserializationError(s"Invalid triple item value: $x")
      }
    }
  }

  private implicit val tripleItemJsonFormat: RootJsonFormat[TripleItem] = new RootJsonFormat[TripleItem] {
    def write(obj: TripleItem): JsValue = obj match {
      case TripleItem.NumberDouble(x) => JsNumber(x)
      case TripleItem.BooleanValue(x) => JsBoolean(x)
      case x: TripleItem.Uri => x.toJson
      case x: TripleItem => JsString(x.toString)
    }

    def read(json: JsValue): TripleItem = json match {
      case JsNumber(x) => TripleItem.Number(x)
      case JsBoolean(x) => TripleItem.BooleanValue(x)
      case s@JsString(x) =>
        val TextPattern = "\"(.*)\"".r
        x match {
          case TextPattern(x) => TripleItem.Text(x)
          case _ => TripleItem.Interval(x).getOrElse(s.convertTo[TripleItem.Uri])
        }
      case x => deserializationError(s"Invalid triple item value: $x")
    }
  }

  private implicit val mappedAtomItemJsonFormat: RootJsonFormat[Atom.Item] = new RootJsonFormat[Atom.Item] {
    def write(obj: Atom.Item): JsValue = obj match {
      case Atom.Item.Variable(x) => JsObject("type" -> JsString("variable"), "value" -> JsString(x))
      case Atom.Item.Constant(x) => JsObject("type" -> JsString("constant"), "value" -> x.toJson)
    }

    def read(json: JsValue): Atom.Item = {
      val fields = json.asJsObject.fields
      val value = fields("value")
      fields("type").convertTo[String] match {
        case "variable" => Atom.Item.Variable(value.convertTo[String])
        case "constant" => Atom.Item.Constant(value.convertTo[TripleItem])
        case x => deserializationError(s"Invalid triple item type: $x")
      }
    }
  }

  private implicit val mappedAtomJsonFormat: RootJsonFormat[Atom] = new RootJsonFormat[Atom] {
    def write(obj: Atom): JsValue = obj match {
      case ResolvedRule.Atom.Basic(s, p, o) => JsObject(
        "subject" -> s.toJson,
        "predicate" -> p.toJson,
        "object" -> o.toJson
      )
      case v@ResolvedRule.Atom.GraphBased(s, p, o) => JsObject(
        "subject" -> s.toJson,
        "predicate" -> p.toJson,
        "object" -> o.toJson,
        "graphs" -> v.graphs.map(_.toJson).toJson
      )
    }

    def read(json: JsValue): Atom = {
      val fields = json.asJsObject.fields
      val s = fields("subject").convertTo[Atom.Item]
      val p = fields("predicate").convertTo[TripleItem.Uri]
      val o = fields("object").convertTo[Atom.Item]
      fields.get("graphs").map(_.convertTo[Set[TripleItem.Uri]]).map(Atom.GraphBased(s, p, o)(_)).getOrElse(Atom.Basic(s, p, o))
    }
  }

  private implicit val measureJsonFormat: RootJsonFormat[Measure] = new RootJsonFormat[Measure] {
    def write(obj: Measure): JsValue = obj match {
      case Measure.BodySize(x) => JsObject("name" -> JsString("BodySize"), "value" -> JsNumber(x))
      case Measure.Confidence(x) => JsObject("name" -> JsString("Confidence"), "value" -> JsNumber(x))
      case Measure.HeadConfidence(x) => JsObject("name" -> JsString("HeadConfidence"), "value" -> JsNumber(x))
      case Measure.HeadCoverage(x) => JsObject("name" -> JsString("HeadCoverage"), "value" -> JsNumber(x))
      case Measure.HeadSize(x) => JsObject("name" -> JsString("HeadSize"), "value" -> JsNumber(x))
      case Measure.Lift(x) => JsObject("name" -> JsString("Lift"), "value" -> JsNumber(x))
      case Measure.PcaBodySize(x) => JsObject("name" -> JsString("PcaBodySize"), "value" -> JsNumber(x))
      case Measure.PcaConfidence(x) => JsObject("name" -> JsString("PcaConfidence"), "value" -> JsNumber(x))
      case Measure.Support(x) => JsObject("name" -> JsString("Support"), "value" -> JsNumber(x))
      case Measure.Cluster(x) => JsObject("name" -> JsString("Cluster"), "value" -> JsNumber(x))
    }

    def read(json: JsValue): Measure = {
      val fields = json.asJsObject.fields
      val value = fields("value")
      fields("name").convertTo[String] match {
        case "BodySize" => Measure.BodySize(value.convertTo[Int])
        case "Confidence" => Measure.Confidence(value.convertTo[Double])
        case "HeadConfidence" => Measure.HeadConfidence(value.convertTo[Double])
        case "HeadCoverage" => Measure.HeadCoverage(value.convertTo[Double])
        case "HeadSize" => Measure.HeadSize(value.convertTo[Int])
        case "Lift" => Measure.Lift(value.convertTo[Double])
        case "PcaBodySize" => Measure.PcaBodySize(value.convertTo[Int])
        case "PcaConfidence" => Measure.PcaConfidence(value.convertTo[Double])
        case "Support" => Measure.Support(value.convertTo[Int])
        case "Cluster" => Measure.Cluster(value.convertTo[Int])
        case x => deserializationError(s"Invalid measure of significance: $x")
      }
    }
  }

  private implicit val resolvedRuleJsonFormat: RootJsonFormat[ResolvedRule] = new RootJsonFormat[ResolvedRule] {
    def write(obj: ResolvedRule): JsValue = JsObject(
      "head" -> obj.head.toJson,
      "body" -> JsArray(obj.body.iterator.map(_.toJson).toVector),
      "measures" -> JsArray(obj.measures.iterator.map(_.toJson).toVector)
    )

    def read(json: JsValue): ResolvedRule = {
      val fields = json.asJsObject.fields
      ResolvedRule(fields("body").convertTo[IndexedSeq[ResolvedRule.Atom]], fields("head").convertTo[ResolvedRule.Atom])(TypedKeyMap(fields("measures").convertTo[Seq[Measure]]))
    }

  }

  implicit def jsonRulesetWriter(source: RulesetSource.Json.type): RulesetWriter = (rules: Traversable[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      writer.println('[')
      for (rule <- rules) {
        writer.println(rule.toJson.prettyPrint)
      }
      writer.println(']')
    } finally {
      writer.close()
    }
  }

  implicit def jsonRulesetReader(source: RulesetSource.Json.type): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      source.mkString.parseJson.convertTo[IndexedSeq[ResolvedRule]]
    } finally {
      source.close()
      is.close()
    }
  }

}
