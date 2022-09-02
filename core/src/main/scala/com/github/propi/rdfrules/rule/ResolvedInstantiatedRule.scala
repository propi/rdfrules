package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.prediction.PredictedResult

import scala.language.implicitConversions

sealed trait ResolvedInstantiatedRule {
  def body: IndexedSeq[ResolvedInstantiatedAtom]

  def head: ResolvedInstantiatedAtom

  def predictedResult: PredictedResult

  def source: ResolvedRule

  def toResolvedRule: ResolvedRule

  def toInstantiatedRule(implicit tripleItemIndex: TripleItemIndex): InstantiatedRule
}

object ResolvedInstantiatedRule {

  private case class Basic(head: ResolvedInstantiatedAtom,
                           body: IndexedSeq[ResolvedInstantiatedAtom],
                           predictedResult: PredictedResult,
                           source: ResolvedRule) extends ResolvedInstantiatedRule {
    def toResolvedRule: ResolvedRule = ResolvedRule(body.map(_.toResolvedAtom), head.toResolvedAtom)

    def toInstantiatedRule(implicit tripleItemIndex: TripleItemIndex): InstantiatedRule = InstantiatedRule(head.toInstantiatedAtom, body.map(_.toInstantiatedAtom), predictedResult, source.toRule)
  }

  def apply(head: ResolvedInstantiatedAtom,
            body: IndexedSeq[ResolvedInstantiatedAtom],
            predictionResult: PredictedResult,
            source: ResolvedRule): ResolvedInstantiatedRule = Basic(head, body, predictionResult, source)

  implicit def apply(rule: InstantiatedRule)(implicit mapper: TripleItemIndex): ResolvedInstantiatedRule = Basic(
    rule.head,
    rule.body.map(ResolvedInstantiatedAtom.apply),
    rule.predictedResult,
    rule.source
  )

}


