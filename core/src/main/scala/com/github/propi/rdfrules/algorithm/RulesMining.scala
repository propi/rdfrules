package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.{Rule, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
trait RulesMining {

  private val _thresholds: TypedKeyMap[Threshold] = TypedKeyMap()
  private val _rulePatterns = collection.mutable.ListBuffer.empty[RulePattern]
  private val _constraints: TypedKeyMap[RuleConstraint] = TypedKeyMap()

  final def addThreshold(threshold: Threshold): RulesMining = {
    _thresholds += threshold
    this
  }

  final def addConstraint(ruleConstraint: RuleConstraint): RulesMining = {
    _constraints += ruleConstraint
    this
  }

  final def addPattern(rulePattern: RulePattern): RulesMining = {
    _rulePatterns += rulePattern
    this
  }

  final def thresholds: TypedKeyMap.Immutable[Threshold] = _thresholds

  final def constraints: TypedKeyMap.Immutable[RuleConstraint] = _constraints

  final def patterns: List[RulePattern] = _rulePatterns.toList

  def mine(implicit tripleIndex: TripleHashIndex): IndexedSeq[Rule.Simple]

}