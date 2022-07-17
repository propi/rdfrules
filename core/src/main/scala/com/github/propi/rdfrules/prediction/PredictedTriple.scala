package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.rule.Rule.FinalRule

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait PredictedTriple {
  def triple: IntTriple

  def rule: FinalRule

  def predictedResult: PredictedResult
}

object PredictedTriple {
  private case class Basic(triple: IntTriple, rule: FinalRule)(val predictedResult: PredictedResult) extends PredictedTriple

  def apply(triple: IntTriple, predictedResult: PredictedResult, rule: FinalRule): PredictedTriple = Basic(triple, rule)(predictedResult)
}


