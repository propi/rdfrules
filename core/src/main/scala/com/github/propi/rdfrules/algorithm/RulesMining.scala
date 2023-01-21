package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.{ForEach, TypedKeyMap}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
abstract class RulesMining(_parallelism: Int, _thresholds: TypedKeyMap.Mutable[Threshold], _constraints: TypedKeyMap.Mutable[RuleConstraint], _patterns: List[RulePattern], _experiment: Boolean) {

  protected def transform(thresholds: TypedKeyMap.Mutable[Threshold] = _thresholds,
                          constraints: TypedKeyMap.Mutable[RuleConstraint] = _constraints,
                          patterns: List[RulePattern] = _patterns,
                          parallelism: Int = _parallelism,
                          experiment: Boolean = _experiment): RulesMining

  final def addThreshold(threshold: Threshold): RulesMining = transform(thresholds = _thresholds += Threshold.validate(threshold))

  final def addConstraint(ruleConstraint: RuleConstraint): RulesMining = transform(constraints = _constraints += ruleConstraint)

  final def addPattern(rulePattern: RulePattern): RulesMining = transform(patterns = rulePattern :: _patterns)

  final def withExperiment: RulesMining = transform(experiment = true)

  final def thresholds: TypedKeyMap[Threshold] = _thresholds

  final def constraints: TypedKeyMap[RuleConstraint] = _constraints

  final def patterns: List[RulePattern] = _patterns

  final def experiment: Boolean = _experiment

  /**
    * Set a parallelism level for main mining task (number of workers).
    * The parallelism should be equal to or lower than the max thread pool size of the execution context
    *
    * @param parallelism number of workers
    * @return
    */
  final def setParallelism(parallelism: Int): RulesMining = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    transform(parallelism = normParallelism)
  }

  final def parallelism: Int = _parallelism

  def mine(ruleConsumer: RuleConsumer)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): ForEach[FinalRule]

}