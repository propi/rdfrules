package com.github.propi.rdfrules.prediction

import spray.json.DefaultJsonProtocol._
import spray.json._

sealed trait PredictedResult extends Product

object PredictedResult {

  case object Positive extends PredictedResult

  case object Negative extends PredictedResult

  case object PcaPositive extends PredictedResult

  implicit val predictedResultJsonFormat: RootJsonFormat[PredictedResult] = new RootJsonFormat[PredictedResult] {
    def write(obj: PredictedResult): JsValue = obj.productPrefix.toJson

    def read(json: JsValue): PredictedResult = {
      val x = json.convertTo[String]
      if (x == Positive.productPrefix) Positive
      else if (x == Negative.productPrefix) Negative
      else if (x == PcaPositive.productPrefix) PcaPositive
      else deserializationError(s"Invalid PredictedResult value '$x'.")
    }
  }

}