package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult

import scala.language.implicitConversions

sealed trait ResolvedInstantiatedRule {
  def body: IndexedSeq[ResolvedInstantiatedAtom]

  def head: ResolvedInstantiatedAtom

  def predictionResult: PredictedResult

  def commingFrom: ResolvedRule

  def toResolvedRule: ResolvedRule
}

object ResolvedInstantiatedRule {

  private case class Basic(head: ResolvedInstantiatedAtom,
                           body: IndexedSeq[ResolvedInstantiatedAtom],
                           predictionResult: PredictedResult,
                           commingFrom: ResolvedRule) extends ResolvedInstantiatedRule {
    def toResolvedRule: ResolvedRule = ResolvedRule(body.map(_.toResolvedAtom), head.toResolvedAtom)
  }

  implicit def apply(rule: InstantiatedRule)(implicit mapper: TripleItemIndex): ResolvedInstantiatedRule = Basic(
    rule.head,
    rule.body.map(ResolvedInstantiatedAtom.apply),
    rule.predictionResult,
    rule.source
  )

}


