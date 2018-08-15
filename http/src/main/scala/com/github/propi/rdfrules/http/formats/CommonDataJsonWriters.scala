package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.{Histogram, Quad, TripleItem, TripleItemType}
import com.github.propi.rdfrules.http.actor.Task.Response
import com.github.propi.rdfrules.ruleset.formats.Json
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 15. 8. 2018.
  */
object CommonDataJsonWriters extends Json {

  implicit val quadWriter: RootJsonWriter[Quad] = (obj: Quad) => JsObject(
    "subject" -> obj.triple.subject.asInstanceOf[TripleItem].toJson,
    "predicate" -> obj.triple.predicate.asInstanceOf[TripleItem].toJson,
    "object" -> obj.triple.`object`.toJson,
    "graph" -> obj.graph.asInstanceOf[TripleItem].toJson
  )

  implicit val tripleItemTypeWriter: RootJsonWriter[TripleItemType] = {
    case TripleItemType.Resource => JsString("Resource")
    case TripleItemType.Text => JsString("Text")
    case TripleItemType.Boolean => JsString("Boolean")
    case TripleItemType.Number => JsString("Number")
    case TripleItemType.Interval => JsString("Interval")
  }

  implicit val typeWriter: RootJsonWriter[(TripleItem.Uri, collection.Map[TripleItemType, Int])] = (obj: (TripleItem.Uri, collection.Map[TripleItemType, Int])) => JsObject(
    "predicate" -> obj._1.asInstanceOf[TripleItem].toJson,
    "types" -> JsArray(obj._2.iterator.map(x => JsObject("type" -> x._1.toJson, "amount" -> x._2.toJson)).toVector)
  )

  implicit val histogramWriter: RootJsonWriter[(Histogram.Key, Int)] = (obj: (Histogram.Key, Int)) => JsObject(
    "subject" -> obj._1.s.map(_.asInstanceOf[TripleItem].toJson).getOrElse(JsNull),
    "predicate" -> obj._1.p.map(_.asInstanceOf[TripleItem].toJson).getOrElse(JsNull),
    "object" -> obj._1.o.map(_.toJson).getOrElse(JsNull),
    "amount" -> obj._2.toJson
  )

  implicit val taskResponseInProgressWriter: RootJsonWriter[Response.InProgress] = (obj: Response.InProgress) => JsObject(
    "id" -> obj.id.toString.toJson,
    "started" -> obj.started.toInstant.toString.toJson,
    "logs" -> JsArray(obj.msg.iterator.map(x => JsObject("time" -> x._2.toInstant.toString.toJson, "message" -> x._1.toJson)).toVector)
  )

  implicit val taskResponseResultWriter: RootJsonWriter[Response.Result] = (obj: Response.Result) => JsObject(
    "id" -> obj.id.toString.toJson,
    "started" -> obj.started.toInstant.toString.toJson,
    "finished" -> obj.finished.toInstant.toString.toJson,
    "logs" -> JsArray(obj.msg.iterator.map(x => JsObject("time" -> x._2.toInstant.toString.toJson, "message" -> x._1.toJson)).toVector)
  )

}
