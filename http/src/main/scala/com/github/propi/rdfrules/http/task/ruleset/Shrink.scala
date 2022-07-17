package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{CommonShrink, ShrinkSetup, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Shrink(shrinkSetup: ShrinkSetup) extends CommonShrink[Ruleset](shrinkSetup) {
  val companion: TaskDefinition = Shrink
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkRuleset"
}