package eu.easyminer.rdf.algorithm

import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.ExtendedRule.ClosedRule
import eu.easyminer.rdf.rule.{Rule, RuleConstraint, RulePattern, Threshold}
import eu.easyminer.rdf.utils.TypedKeyMap

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