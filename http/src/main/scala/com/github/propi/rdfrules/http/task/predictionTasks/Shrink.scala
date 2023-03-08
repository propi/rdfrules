package com.github.propi.rdfrules.http.task.predictionTasks

import com.github.propi.rdfrules.http.task.{CommonShrink, ShrinkSetup, TaskDefinition}
import com.github.propi.rdfrules.prediction.PredictionTasksResults

class Shrink(shrinkSetup: ShrinkSetup) extends CommonShrink[PredictionTasksResults](shrinkSetup) {
  val companion: TaskDefinition = Shrink
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkPredictionTasks"
}