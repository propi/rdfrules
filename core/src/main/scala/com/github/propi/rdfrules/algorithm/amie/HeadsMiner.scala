package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.Settings
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}

import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class HeadsMiner private(_parallelism: Int = Runtime.getRuntime.availableProcessors(),
                         _thresholds: TypedKeyMap[Threshold] = TypedKeyMap(),
                         _constraints: TypedKeyMap[RuleConstraint] = TypedKeyMap(),
                         _patterns: List[RulePattern] = Nil)
                        (implicit debugger: Debugger) extends RulesMining(_parallelism, _thresholds, _constraints, _patterns) {

  self =>

  protected def transform(thresholds: TypedKeyMap[Threshold],
                          constraints: TypedKeyMap[RuleConstraint],
                          patterns: List[RulePattern],
                          parallelism: Int): RulesMining = new HeadsMiner(parallelism, thresholds, constraints, patterns)

  def mine(ruleConsumer: RuleConsumer)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): RuleConsumer.Result = {
    //val logger = debugger.logger
    //create amie process with debugger and final triple index
    implicit val settings: RuleRefinement.Settings = new Settings(this)(/*if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) */ debugger /* else Debugger.EmptyDebugger*/ , mapper)
    val process = new AmieProcess
    process.getHeads.foreach(ruleConsumer.send(_))
    ruleConsumer.result
  }

  private class AmieProcess(implicit val tripleIndex: TripleIndex[Int], val settings: RuleRefinement.Settings, val forAtomMatcher: MappedAtomPatternMatcher[Atom]) extends HeadsFetcher

}

object HeadsMiner {

  /**
    * Create a head miner. If you do not specify any threshold, default is minHeadSize = 100, minSupport = 1, maxRuleLength = 3
    *
    * @param debugger debugger
    * @return
    */
  def apply()(implicit debugger: Debugger): RulesMining = new HeadsMiner()

}