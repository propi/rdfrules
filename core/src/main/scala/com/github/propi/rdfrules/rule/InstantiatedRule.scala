package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.rule.Rule.FinalRule

sealed trait InstantiatedRule {
  def body: IndexedSeq[InstantiatedAtom]

  def head: InstantiatedAtom

  def predictionResult: PredictedResult

  def source: FinalRule

  def toRule: FinalRule
}

object InstantiatedRule {

  sealed trait PredictedResult

  object PredictedResult {

    case object Positive extends PredictedResult

    case object Negative extends PredictedResult

    case object PcaPositive extends PredictedResult

  }

  private case class Basic(head: InstantiatedAtom, body: IndexedSeq[InstantiatedAtom], predictionResult: PredictedResult, source: FinalRule) extends InstantiatedRule {
    def toRule: FinalRule = Rule(head.toAtom, body.map(_.toAtom))
  }

  def apply(head: InstantiatedAtom, body: IndexedSeq[InstantiatedAtom], predictionResult: PredictedResult, source: FinalRule): InstantiatedRule = {
    Basic(head, body, predictionResult, source)
  }

  //implicit def instantiatedRuleToRule(instantiatedRule: InstantiatedRule): Rule = Rule.Simple()
  //case class Simple(head: Atom, body:)

}
