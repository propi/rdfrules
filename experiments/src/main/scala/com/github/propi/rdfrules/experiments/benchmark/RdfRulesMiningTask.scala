package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.{Measure, ResolvedRule, RuleConstraint, Threshold}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait RdfRulesMiningTask[T] extends Task[Index, Index, Ruleset, T] with TaskPreProcessor[Index, Index] with DefaultMiningSettings {

  self: TaskPostProcessor[Ruleset, T] =>

  implicit val debugger: Debugger

  val withConstantsAtTheObjectPosition: Boolean = false
  val countLift: Boolean = false

  private def createDefaultMiningTask: RulesMining = Function.chain[RulesMining](List(
    _.setParallelism(numberOfThreads),
    _.addThreshold(Threshold.MinHeadCoverage(minHeadCoverage)),
    _.addThreshold(Threshold.MaxRuleLength(maxRuleLength)),
    x => if (injectiveMapping) x.addConstraint(RuleConstraint.InjectiveMapping()) else x,
    x => if (allowConstants) {
      if (constantsEverywhere) {
        x
      } else if (withConstantsAtTheObjectPosition) {
        x.addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Object))
      } else {
        x.addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.LowerCardinalitySide))
      }
    } else {
      x.addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
    },
    x => if (experiment) x.withExperiment else x,
    x => if (withoutDuplicatePredicates) x.addConstraint(RuleConstraint.WithoutDuplicatePredicates()) else x
  ))(Amie())

  protected def miningTask(rulesMining: RulesMining): RulesMining = rulesMining

  protected def preProcess(input: Index): Index = input

  protected def countOtherMetrics(ruleset: Ruleset): Ruleset = {
    Function.chain[Ruleset](List(
      _.setParallelism(numberOfThreads),
      x => if (minConfidence <= 0.0) x else x.computeConfidence(minConfidence, injectiveMapping).cache,
      x => if (minPcaConfidence <= 0.0) x else x.computePcaConfidence(minPcaConfidence, injectiveMapping).cache,
      x => if (!countLift || minConfidence <= 0.0) x else x.computeLift(minConfidence, injectiveMapping).cache,
      x => if (skylinePruning) x.onlyBetterDescendant(Measure.PcaConfidence).cache else x,
    ))(ruleset.withoutQuasiBinding(injectiveMapping).cache)
  }

  protected def taskBody(input: Index): Ruleset = {
    val res = countOtherMetrics(input.mine(miningTask(createDefaultMiningTask), ruleConsumer))
    res.size
    res
  }

}

object RdfRulesMiningTask {

  trait GetRulesPostProcessor extends TaskPreProcessor[Ruleset, IndexedSeq[ResolvedRule]] {
    protected def preProcess(input: Ruleset): IndexedSeq[ResolvedRule] = input.resolvedRules.toIndexedSeq
  }

}