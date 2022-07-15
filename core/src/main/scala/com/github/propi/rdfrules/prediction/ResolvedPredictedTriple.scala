package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.rule.ResolvedRule

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait ResolvedPredictedTriple {
  def triple: Triple

  def rules: Set[ResolvedRule]

  def predictedResult: PredictedResult
}

object ResolvedPredictedTriple {

  private case class Basic(triple: Triple)(val rules: Set[ResolvedRule], val predictedResult: PredictedResult) extends ResolvedPredictedTriple

  def apply(triple: Triple, predictedResult: PredictedResult, rules: Set[ResolvedRule]): ResolvedPredictedTriple = Basic(triple)(rules, predictedResult)

  implicit def apply(predictedTriple: PredictedTriple)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple = apply(
    Triple(
      mapper.getTripleItem(predictedTriple.triple.subject).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(predictedTriple.triple.predicate).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(predictedTriple.triple.`object`)
    ),
    predictedTriple.predictedResult,
    predictedTriple.rules.map(ResolvedRule(_))
  )

}
