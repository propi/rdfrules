package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 18. 5. 2019.
  */
trait RulesetTask[T] extends Task[Ruleset, Ruleset, Ruleset, T] with TaskPreProcessor[Ruleset, Ruleset] with DefaultMiningSettings {

  self: TaskPostProcessor[Ruleset, T] =>

}