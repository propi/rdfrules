package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.rule.Rule.FinalRule

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait PredictedTriple {
  def triple: IntTriple

  def rules: Set[FinalRule]

  def predictedResult: PredictedResult
}

object PredictedTriple {
  private case class Basic(triple: IntTriple)(val rules: Set[FinalRule], val predictedResult: PredictedResult) extends PredictedTriple

  def apply(triple: IntTriple, predictedResult: PredictedResult, rule: FinalRule): PredictedTriple = apply(triple, predictedResult, Set(rule))

  def apply(triple: IntTriple, predictedResult: PredictedResult, rules: Set[FinalRule]): PredictedTriple = Basic(triple)(rules, predictedResult)
}


