package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.Properties.PropertyStats
import com.github.propi.rdfrules.data.{Histogram, Quad, Triple, TripleItemType}
import com.github.propi.rdfrules.http.formats.CommonDataJsonFormats._
import com.github.propi.rdfrules.http.service.Task.TaskResponse
import com.github.propi.rdfrules.prediction.ResolvedPredictedTriple
import com.github.propi.rdfrules.rule.{ResolvedInstantiatedAtom, ResolvedInstantiatedRule}
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 15. 8. 2018.
  */
object CommonDataJsonWriters {

  implicit val evaluationResultWriter: RootJsonWriter[CompletenessEvaluationResult] = (obj: CompletenessEvaluationResult) => JsObject(
    "tp" -> obj.tp.toJson,
    "fp" -> obj.fp.toJson,
    "fn" -> obj.fn.toJson,
    "accuracy" -> obj.accuracy.toJson,
    "precision" -> obj.precision.toJson,
    "recall" -> obj.recall.toJson,
    "fscore" -> obj.fscore.toJson
  )

  implicit val quadWriter: RootJsonWriter[Quad] = (obj: Quad) => JsObject(
    "subject" -> obj.triple.subject.toJson,
    "predicate" -> obj.triple.predicate.toJson,
    "object" -> obj.triple.`object`.toJson,
    "graph" -> obj.graph.toJson
  )

  implicit val tripleWriter: RootJsonWriter[Triple] = (obj: Triple) => JsObject(
    "subject" -> obj.subject.toJson,
    "predicate" -> obj.predicate.toJson,
    "object" -> obj.`object`.toJson
  )

  implicit val tripleItemTypeWriter: RootJsonWriter[TripleItemType] = {
    case TripleItemType.Uri => JsString("Uri")
    case TripleItemType.Text => JsString("Text")
    case TripleItemType.Boolean => JsString("Boolean")
    case TripleItemType.Number => JsString("Number")
    case TripleItemType.Interval => JsString("Interval")
  }

  /*implicit val typeWriter: RootJsonWriter[(TripleItem.Uri, collection.Map[TripleItemType, Int])] = (obj: (TripleItem.Uri, collection.Map[TripleItemType, Int])) => JsObject(
    "predicate" -> obj._1.asInstanceOf[TripleItem].toJson,
    "types" -> JsArray(obj._2.iterator.map(x => JsObject("name" -> x._1.toJson, "amount" -> x._2.toJson)).toVector)
  )*/

  implicit val histogramWriter: RootJsonWriter[(Histogram.Key, Int)] = (obj: (Histogram.Key, Int)) => JsObject(
    "subject" -> obj._1.s.map(_.toJson).getOrElse(JsNull),
    "predicate" -> obj._1.p.map(_.toJson).getOrElse(JsNull),
    "object" -> obj._1.o.map(_.toJson).getOrElse(JsNull),
    "amount" -> obj._2.toJson
  )

  implicit val predictedTripleWriter: RootJsonWriter[ResolvedPredictedTriple] = (obj: ResolvedPredictedTriple) => JsObject(
    "triple" -> obj.triple.toJson,
    "predictedResult" -> obj.predictedResult.toJson,
    "rules" -> obj.rules.toJson
  )

  implicit val resolvedInstantiatedAtomWriter: RootJsonWriter[ResolvedInstantiatedAtom] = (obj: ResolvedInstantiatedAtom) => JsObject(
    "subject" -> obj.subject.toJson,
    "predicate" -> obj.predicate.toJson,
    "object" -> obj.`object`.toJson
  )

  implicit val resolvedInstantiatedRuleWriter: RootJsonWriter[ResolvedInstantiatedRule] = (obj: ResolvedInstantiatedRule) => JsObject(
    "head" -> obj.head.toJson,
    "body" -> JsArray(obj.body.iterator.map(_.toJson).toVector),
    "source" -> obj.source.toJson,
    "predictedResult" -> obj.predictedResult.toJson
  )

  implicit val propertyStatsWriter: RootJsonWriter[PropertyStats] = (obj: PropertyStats) => obj.iterator.map(x => JsObject("name" -> x._1.toJson, "amount" -> x._2.toJson)).toSeq.toJson

  implicit val taskResponseInProgressWriter: RootJsonWriter[TaskResponse.InProgress] = (obj: TaskResponse.InProgress) => JsObject(
    "id" -> obj.id.toString.toJson,
    "started" -> obj.started.toInstant.toString.toJson,
    "logs" -> JsArray(obj.msg.iterator.map(x => JsObject("time" -> x._2.toInstant.toString.toJson, "message" -> x._1.toJson)).toVector)
  )

  implicit val taskResponseResultWriter: RootJsonWriter[TaskResponse.Result] = (obj: TaskResponse.Result) => JsObject(
    "id" -> obj.id.toString.toJson,
    "started" -> obj.started.toInstant.toString.toJson,
    "finished" -> obj.finished.toInstant.toString.toJson,
    "logs" -> JsArray(obj.msg.iterator.map(x => JsObject("time" -> x._2.toInstant.toString.toJson, "message" -> x._1.toJson)).toVector)
  )

}
