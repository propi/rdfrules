package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.http.task.{CommonShrink, ShrinkSetup, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictedTriples

class Shrink(shrinkSetup: ShrinkSetup) extends CommonShrink[PredictedTriples](shrinkSetup) {
  val companion: TaskDefinition = Shrink
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkPrediction"
}