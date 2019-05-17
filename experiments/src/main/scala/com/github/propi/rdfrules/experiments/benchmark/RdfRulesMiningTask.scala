package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.{RuleConstraint, Threshold}
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset}
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
trait RdfRulesMiningTask[T] extends Task[Index, Index, Ruleset, T] with TaskPreProcessor[Index, Index] with DefaultMiningSettings {

  self: TaskPostProcessor[Ruleset, T] =>

  implicit val debugger: Debugger
  val withConstantsAtTheObjectPosition: Boolean = false

  val minLift: Double = 0.0

  private def createDefaultMiningTask: RulesMining = Function.chain[RulesMining](List(
    _.addThreshold(Threshold.MinHeadCoverage(minHeadCoverage)),
    _.addThreshold(Threshold.MaxRuleLength(maxRuleLength)),
    x => if (allowConstants) {
      if (withConstantsAtTheObjectPosition) x.addConstraint(RuleConstraint.WithInstances(true)) else x.addConstraint(RuleConstraint.WithInstances(false))
    } else {
      x
    }
  ))(Amie(numberOfThreads))

  protected def miningTask(rulesMining: RulesMining): RulesMining = rulesMining

  protected def preProcess(input: Index): Index = input

  protected def countOtherMetrics(ruleset: Ruleset): Ruleset = {
    Function.chain[Ruleset](List(
      x => if (minConfidence <= 0.0) x else x.computeConfidence(minConfidence),
      x => if (minPcaConfidence <= 0.0) x else x.computePcaConfidence(minPcaConfidence),
      x => if (minLift <= 0.0 || minConfidence <= 0.0) x else x.computeLift(minConfidence)
    ))(ruleset)
  }

  protected def taskBody(input: Index): Ruleset = countOtherMetrics(input.mine(miningTask(createDefaultMiningTask)))

}

object RdfRulesMiningTask {

  trait GetRulesPostProcessor extends TaskPreProcessor[Ruleset, IndexedSeq[ResolvedRule]] {
    protected def preProcess(input: Ruleset): IndexedSeq[ResolvedRule] = input.resolvedRules.toIndexedSeq
  }

}