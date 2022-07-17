package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.prediction.{PredictedResult, ResolvedPredictedTriple}
import com.github.propi.rdfrules.rule.ResolvedRule

sealed trait GroupedPredictedTriple {
  def triple: Triple

  def rules: Iterable[ResolvedRule]

  def predictedResult: PredictedResult

  def :+(rules: Iterable[ResolvedRule]): GroupedPredictedTriple
}

object GroupedPredictedTriple {

  private case class Basic(triple: Triple)(val rules: Iterable[ResolvedRule], val predictedResult: PredictedResult) extends GroupedPredictedTriple {
    def :+(rules: Iterable[ResolvedRule]): GroupedPredictedTriple = copy()(this.rules.concat(rules), predictedResult)
  }

  def apply(resolvedPredictedTriple: ResolvedPredictedTriple): GroupedPredictedTriple = {
    Basic(resolvedPredictedTriple.triple)(Vector(resolvedPredictedTriple.rule), resolvedPredictedTriple.predictedResult)
  }

}