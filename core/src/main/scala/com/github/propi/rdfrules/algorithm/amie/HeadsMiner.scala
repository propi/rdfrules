package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.Settings
import com.github.propi.rdfrules.index.{TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class HeadsMiner private(_parallelism: Int = Runtime.getRuntime.availableProcessors(),
                         _thresholds: TypedKeyMap[Threshold] = TypedKeyMap(),
                         _constraints: TypedKeyMap[RuleConstraint] = TypedKeyMap(),
                         _patterns: List[RulePattern] = Nil)
                        (implicit debugger: Debugger, ec: ExecutionContext) extends RulesMining(_parallelism, _thresholds, _constraints, _patterns) {

  self =>

  protected def transform(thresholds: TypedKeyMap[Threshold],
                          constraints: TypedKeyMap[RuleConstraint],
                          patterns: List[RulePattern],
                          parallelism: Int): RulesMining = new HeadsMiner(parallelism, thresholds, constraints, patterns)

  def mine(implicit tripleIndex: TripleHashIndex[Int], mapper: TripleItemHashIndex): IndexedSeq[Rule.Simple] = {
    val logger = debugger.logger
    //create amie process with debugger and final triple index
    implicit val settings: RuleRefinement.Settings = new Settings(this)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger, mapper)
    val process = new AmieProcess
    process.getHeads.map(Rule.Simple.apply)
  }

  private class AmieProcess(implicit val tripleIndex: TripleHashIndex[Int], val settings: RuleRefinement.Settings, val forAtomMatcher: AtomPatternMatcher[Atom], val ec: ExecutionContext) extends HeadsFetcher {
    val patterns: List[RulePattern] = self.patterns
    val thresholds: TypedKeyMap.Immutable[Threshold] = self.thresholds
  }

}

object HeadsMiner {

  def apply()(implicit debugger: Debugger, ec: ExecutionContext): RulesMining = {
    new HeadsMiner()
      .addThreshold(Threshold.MinHeadSize(100))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
      .addThreshold(Threshold.MaxRuleLength(3))
  }

}