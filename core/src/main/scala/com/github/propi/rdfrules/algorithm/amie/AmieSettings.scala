package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.index.{IntervalsIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.ExpandingRule.HeadTriplesBootstrapper
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsForPredicates
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.Debugger

class AmieSettings(rulesMining: RulesMining, val bootstrapper: Option[HeadTriplesBootstrapper])(implicit debugger: Debugger, mapper: TripleItemIndex, val intervals: IntervalsIndex) {
  @volatile private var _minHeadCoverage: Double = rulesMining.thresholds.get[Threshold.MinHeadCoverage].map(_.value).getOrElse(0.0)

  val parallelism: Int = rulesMining.parallelism
  val injectiveMapping: Boolean = rulesMining.constraints.exists[RuleConstraint.InjectiveMapping]
  val patterns: List[RulePattern.Mapped] = rulesMining.patterns.map(_.mapped)
  val minHeadSize: Int = rulesMining.thresholds.get[Threshold.MinHeadSize].map(_.value).getOrElse(100)
  val minSupport: Int = rulesMining.thresholds.get[Threshold.MinSupport].map(_.value).getOrElse(1)
  val minAtomSize: Int = rulesMining.thresholds.get[Threshold.MinAtomSize].map(_.value).getOrElse(0)
  val constantsForPredicates: Map[Int, ConstantsForPredicates.ConstantsForPredicatePosition] = rulesMining.constraints.get[RuleConstraint.ConstantsForPredicates].map(_.mapped).getOrElse(Map.empty)
  val constantsPosition: Option[ConstantsPosition] = rulesMining.constraints.get[RuleConstraint.ConstantsAtPosition].map(_.position)
  //val isWithInstances: Int => Boolean = if (constantsPosition.contains(Nowhere))!constantsPosition.contains(ConstantsPosition.Nowhere) || constantsForPredicates.nonEmpty
  val maxRuleLength: Int = rulesMining.thresholds.get[Threshold.MaxRuleLength].map(_.value).getOrElse(3)
  val maxDanglingVariables: Int = math.floor((maxRuleLength * 2) / 3).toInt
  val withDuplicitPredicates: Boolean = !rulesMining.constraints.exists[RuleConstraint.WithoutDuplicatePredicates]
  val filters: List[RuleConstraint.MappedFilter] = rulesMining.constraints.iterator.collect {
    case filter: RuleConstraint.Filter => filter.mapped
  }.toList
  val timeout: Option[Long] = rulesMining.thresholds.get[Threshold.Timeout].map(_.duration.toMillis)
  private val startTime = System.currentTimeMillis()
  val anytimeRefinement: AnytimeRefinement = timeout -> rulesMining.thresholds.get[Threshold.LocalTimeout] match {
    case (Some(duration), Some(localTimeout)) => new AnytimeRefinement.GlobalTimeout(startTime + duration) :: new AnytimeRefinement.LocalTimeout(localTimeout)
    case (Some(duration), None) => new AnytimeRefinement.GlobalTimeout(startTime + duration)
    case (None, Some(localTimeout)) => new AnytimeRefinement.LocalTimeout(localTimeout)
    case (None, None) => AnytimeRefinement.Empty
  }

  def currentDuration: Long = System.currentTimeMillis() - startTime

  private val onlyPredicates = rulesMining.constraints.get[RuleConstraint.OnlyPredicates].map(_.mapped)
  private val withoutPredicates = rulesMining.constraints.get[RuleConstraint.WithoutPredicates].map(_.mapped)

  //def test(newAtom: Atom, rule: Option[Rule]): Boolean = filters.forall(_.test(newAtom, rule))

  //def test(newAtom: Atom, rule: Rule): Boolean = test(newAtom, Some(rule))

  def test(newAtom: Atom): Boolean = filters.forall(_.test(newAtom, None))

  def isValidPredicate(predicate: Int): Boolean = onlyPredicates.forall(_ (predicate)) && withoutPredicates.forall(!_ (predicate))

  def minHeadCoverage: Double = _minHeadCoverage

  def minComputedSupport(rule: ExpandingRule): Double = math.max(rule.headSize * minHeadCoverage, minSupport)

  def setMinHeadCoverage(value: Double): Unit = _minHeadCoverage = value

  def experiment: Boolean = rulesMining.experiment

  override def toString: String = s"MinHeadSize=$minHeadSize,\n" +
    s"MinHeadCoverage=$minHeadCoverage,\n" +
    s"MinSupport=$minSupport,\n" +
    s"MaxThreads=$parallelism,\n" +
    s"MinAtomSize=$minAtomSize,\n" +
    s"MaxRuleLength=$maxRuleLength,\n" +
    s"ConstantsForPredicates=${constantsForPredicates.iterator.map(x => mapper.getTripleItem(x._1) -> x._2).toList},\n" +
    s"ConstantsPosition=${constantsPosition.map(_.toString).getOrElse("All")},\n" +
    s"Timeout=${timeout.getOrElse(-1L)},\n" +
    s"LocalTimeout=${rulesMining.thresholds.get[Threshold.LocalTimeout]},\n" +
    s"WithDuplicitPredicates=$withDuplicitPredicates,\n" +
    s"InjectiveMapping=$injectiveMapping,\n" +
    s"Patterns=${rulesMining.patterns},\n" +
    s"OnlyPredicates=${onlyPredicates.iterator.flatten.map(mapper.getTripleItem).toList},\n" +
    s"WithoutPredicates=${withoutPredicates.iterator.flatten.map(mapper.getTripleItem).toList}\n" +
    s"Experiment=$experiment"
}
