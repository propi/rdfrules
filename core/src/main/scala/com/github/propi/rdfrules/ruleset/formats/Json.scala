package com.github.propi.rdfrules.ruleset.formats

import java.io.{BufferedInputStream, OutputStreamWriter, PrintWriter}
import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.PropertyCardinalities
import com.github.propi.rdfrules.rule.{Measure, ResolvedRule}
import com.github.propi.rdfrules.rule.ResolvedAtom
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder, TypedKeyMap}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.io.Source
import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object Json {

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

  private implicit val mappedAtomItemJsonFormat: RootJsonFormat[ResolvedItem] = new RootJsonFormat[ResolvedItem] {
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

  private implicit val mappedAtomJsonFormat: RootJsonFormat[ResolvedAtom] = new RootJsonFormat[ResolvedAtom] {
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

  implicit val resolvedRuleJsonFormat: RootJsonFormat[ResolvedRule] = new RootJsonFormat[ResolvedRule] {
    def write(obj: ResolvedRule): JsValue = JsObject(
      "head" -> obj.head.toJson,
      "body" -> JsArray(obj.body.iterator.map(_.toJson).toVector),
      "measures" -> JsArray(obj.measures.iterator.map(_.toJson).toVector)
    )

    def read(json: JsValue): ResolvedRule = {
      val fields = json.asJsObject.fields
      ResolvedRule(fields("body").convertTo[IndexedSeq[ResolvedAtom]], fields("head").convertTo[ResolvedAtom])(TypedKeyMap(fields("measures").convertTo[Seq[Measure]]))
    }
  }

  implicit val propertyCardinalitiesWriter: JsonWriter[PropertyCardinalities.Resolved] = (obj: PropertyCardinalities.Resolved) => JsObject(
    "property" -> obj.property.toJson,
    "size" -> obj.size.toJson,
    "domain" -> obj.domain.toJson,
    "range" -> obj.range.toJson
  )

  implicit def jsonRulesetWriter(source: RulesetSource.Json.type): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      writer.println('[')
      rules.map(rule => rule.toJson.prettyPrint).foldLeft("") { (sep, rule) =>
        writer.println(sep + rule)
        ","
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
      ForEach.from(source.mkString.parseJson.convertTo[IndexedSeq[ResolvedRule]])
    } finally {
      source.close()
      is.close()
    }
  }

}