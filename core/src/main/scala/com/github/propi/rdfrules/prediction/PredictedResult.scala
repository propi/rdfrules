package com.github.propi.rdfrules.prediction

sealed trait PredictedResult

object PredictedResult {

  case object Positive extends PredictedResult

  case object Negative extends PredictedResult

  case object PcaPositive extends PredictedResult

}