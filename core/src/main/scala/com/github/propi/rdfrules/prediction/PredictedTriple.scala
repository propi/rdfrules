package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.rule.InstantiatedAtom
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.rule.Rule.FinalRule

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait PredictedTriple {
  def triple: InstantiatedAtom

  def rules: Set[FinalRule]

  def predictedResult: PredictedResult
}

object PredictedTriple {
  private case class Basic(triple: InstantiatedAtom)(val rules: Set[FinalRule], val predictedResult: PredictedResult) extends PredictedTriple

  def apply(triple: InstantiatedAtom, predictedResult: PredictedResult, rule: FinalRule): PredictedTriple = apply(triple, predictedResult, Set(rule))

  def apply(triple: InstantiatedAtom, predictedResult: PredictedResult, rules: Set[FinalRule]): PredictedTriple = Basic(triple)(rules, predictedResult)
}


