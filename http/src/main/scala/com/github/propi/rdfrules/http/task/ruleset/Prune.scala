package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.ruleset.Prune.PruningStrategy
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.TypedKeyMap.Key

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Prune(strategy: PruningStrategy)(implicit debugger: Debugger) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Prune

  def execute(input: Ruleset): Ruleset = strategy match {
    case x: PruningStrategy.DataCoveragePruning => input.pruned(x.onlyExistingTriples, x.onlyFunctionalProperties, x.injectiveMapping)
    case PruningStrategy.OnlyBetterDescendant(measure) => input.onlyBetterDescendant(measure)
    case PruningStrategy.Closed(measure) => input.closed(measure)
    case PruningStrategy.Maximal => input.maximal
  }
}

object Prune extends TaskDefinition {
  val name: String = "Prune"

  sealed trait PruningStrategy

  object PruningStrategy {

    case class DataCoveragePruning(onlyFunctionalProperties: Boolean, onlyExistingTriples: Boolean, injectiveMapping: Boolean) extends PruningStrategy

    case object Maximal extends PruningStrategy

    case class Closed(measure: Key[Measure]) extends PruningStrategy

    case class OnlyBetterDescendant(measure: Key[Measure]) extends PruningStrategy

  }
}