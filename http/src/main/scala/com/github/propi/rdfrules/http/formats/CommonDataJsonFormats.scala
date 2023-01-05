package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.Prefix
import com.github.propi.rdfrules.http.task.CompletionStrategy
import com.github.propi.rdfrules.http.task.ruleset.Prune.PruningStrategy
import com.github.propi.rdfrules.prediction.PredictedResult
import com.github.propi.rdfrules.utils.BasicFunctions
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

  implicit val completionStrategyFormat: RootJsonFormat[CompletionStrategy] = new RootJsonFormat[CompletionStrategy] {
    def write(obj: CompletionStrategy): JsValue = BasicFunctions.firstToLowerCase(obj.productPrefix).toJson

    def read(json: JsValue): CompletionStrategy = {
      val name = json.convertTo[String]
      List(
        CompletionStrategy.PcaPredictions,
        CompletionStrategy.QpcaPredictions,
        CompletionStrategy.DistinctPredictions,
        CompletionStrategy.FunctionalPredictions
      ).find(x => BasicFunctions.firstToLowerCase(x.productPrefix) == name).getOrElse(deserializationError(s"Invalid completion strategy name: $name"))
    }
  }

  implicit val dataCoveragePruningFormat: RootJsonFormat[PruningStrategy.DataCoveragePruning] = jsonFormat3(PruningStrategy.DataCoveragePruning)

}