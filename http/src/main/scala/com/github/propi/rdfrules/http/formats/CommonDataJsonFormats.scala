package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.http.task.ruleset.Prune.PruningStrategy
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictionTask, PredictionTaskResult}
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.JsonSelector.PimpedJsValue
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

/**
  * Created by Vaclav Zeman on 15. 8. 2018.
  */
object CommonDataJsonFormats {

  implicit val prefixFullFormat: RootJsonFormat[Prefix.Full] = jsonFormat2(Prefix.Full)

  implicit val prefixNamespaceFormat: RootJsonFormat[Prefix.Namespace] = jsonFormat1(Prefix.Namespace)

  implicit val prefixFormat: RootJsonFormat[Prefix] = new RootJsonFormat[Prefix] {
    def write(obj: Prefix): JsValue = obj match {
      case x: Prefix.Full => x.toJson
      case x: Prefix.Namespace => x.toJson
    }

    def read(json: JsValue): Prefix = Try(json.convertTo[Prefix.Full]).getOrElse(json.convertTo[Prefix.Namespace])
  }

  implicit val predictedResultFormat: RootJsonFormat[PredictedResult] = new RootJsonFormat[PredictedResult] {
    def read(json: JsValue): PredictedResult = json.convertTo[String] match {
      case "Positive" => PredictedResult.Positive
      case "Negative" => PredictedResult.Negative
      case "PcaPositive" => PredictedResult.PcaPositive
      case x => deserializationError(s"Invalid predicted result name: $x")
    }

    def write(obj: PredictedResult): JsValue = obj match {
      case PredictedResult.Positive => "Positive".toJson
      case PredictedResult.Negative => "Negative".toJson
      case PredictedResult.PcaPositive => "PcaPositive".toJson
    }
  }

  implicit val dataCoveragePruningFormat: RootJsonFormat[PruningStrategy.DataCoveragePruning] = jsonFormat3(PruningStrategy.DataCoveragePruning)

  implicit val tripleItemPositionFormat: RootJsonFormat[TripleItemPosition[TripleItem]] = new RootJsonFormat[TripleItemPosition[TripleItem]] {
    def read(json: JsValue): TripleItemPosition[TripleItem] = {
      val selector = json.toSelector
      selector.get("s").toOpt[TripleItem].map(TripleItemPosition.Subject(_))
        .orElse(selector.get("o").toOpt[TripleItem].map(TripleItemPosition.Object(_)))
        .getOrElse(deserializationError("Missing triple item position."))
    }

    def write(obj: TripleItemPosition[TripleItem]): JsValue = obj match {
      case TripleItemPosition.Subject(x) => JsObject("s" -> x.toJson)
      case TripleItemPosition.Object(x) => JsObject("o" -> x.toJson)
    }
  }

  implicit val predictionTaskFormat: RootJsonFormat[PredictionTask.Resolved] = jsonFormat2(PredictionTask.Resolved.apply)

  implicit val predictionTaskResultWriter: RootJsonFormat[PredictionTaskResult.Resolved] = jsonFormat2(PredictionTaskResult.Resolved.apply)

}