package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.{RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
abstract class RulesMining(_parallelism: Int, _thresholds: TypedKeyMap[Threshold], _constraints: TypedKeyMap[RuleConstraint], _patterns: List[RulePattern]) {

  protected def transform(thresholds: TypedKeyMap[Threshold] = _thresholds,
                          constraints: TypedKeyMap[RuleConstraint] = _constraints,
                          patterns: List[RulePattern] = _patterns,
                          parallelism: Int = _parallelism): RulesMining

  final def addThreshold(threshold: Threshold): RulesMining = transform(thresholds = _thresholds + Threshold.validate(threshold))

  final def addConstraint(ruleConstraint: RuleConstraint): RulesMining = transform(constraints = _constraints + ruleConstraint)

  final def addPattern(rulePattern: RulePattern): RulesMining = transform(patterns = rulePattern :: _patterns)

  final def thresholds: TypedKeyMap.Immutable[Threshold] = _thresholds

  final def constraints: TypedKeyMap.Immutable[RuleConstraint] = _constraints

  final def patterns: List[RulePattern] = _patterns

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

  def mine(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): RuleConsumer.Result

}