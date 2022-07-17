package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait ResolvedPredictedTriple {
  def triple: Triple

  def rule: ResolvedRule

  def predictedResult: PredictedResult

  def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple
}

object ResolvedPredictedTriple {

  private case class Basic(triple: Triple, rule: ResolvedRule)(val predictedResult: PredictedResult) extends ResolvedPredictedTriple {
    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple = PredictedTriple(
      triple.toIndexedTriple,
      predictedResult,
      rule.toRule
    )
  }

  def apply(triple: Triple, predictedResult: PredictedResult, rule: ResolvedRule): ResolvedPredictedTriple = Basic(triple, rule)(predictedResult)

  implicit def apply(predictedTriple: PredictedTriple)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple = apply(
    Triple(
      mapper.getTripleItem(predictedTriple.triple.s).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(predictedTriple.triple.p).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(predictedTriple.triple.o)
    ),
    predictedTriple.predictedResult,
    ResolvedRule(predictedTriple.rule)
  )

}
