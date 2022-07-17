package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{CommonShrink, ShrinkSetup, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Shrink(shrinkSetup: ShrinkSetup) extends CommonShrink[Dataset](shrinkSetup) {
  val companion: TaskDefinition = Shrink
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkQuads"
}