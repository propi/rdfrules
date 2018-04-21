package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.{Rule, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
abstract class RulesMining(_thresholds: TypedKeyMap[Threshold], _constraints: TypedKeyMap[RuleConstraint], _patterns: List[RulePattern]) {

  protected def transform(thresholds: TypedKeyMap[Threshold] = _thresholds,
                          constraints: TypedKeyMap[RuleConstraint] = _constraints,
                          patterns: List[RulePattern] = _patterns): RulesMining

  final def addThreshold(threshold: Threshold): RulesMining = transform(thresholds = _thresholds + threshold)

  final def addConstraint(ruleConstraint: RuleConstraint): RulesMining = transform(constraints = _constraints + ruleConstraint)

  final def addPattern(rulePattern: RulePattern): RulesMining = transform(patterns = rulePattern :: _patterns)

  final def thresholds: TypedKeyMap.Immutable[Threshold] = _thresholds

  final def constraints: TypedKeyMap.Immutable[RuleConstraint] = _constraints

  final def patterns: List[RulePattern] = _patterns

  def mine(implicit tripleIndex: TripleHashIndex): IndexedSeq[Rule.Simple]

}