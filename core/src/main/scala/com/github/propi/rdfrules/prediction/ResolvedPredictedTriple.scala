package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.JsonSelector.PimpedJsValue
import spray.json._

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

  implicit val resolvedPredictedTripleJsonFormat: RootJsonFormat[ResolvedPredictedTriple] = new RootJsonFormat[ResolvedPredictedTriple] {
    def write(obj: ResolvedPredictedTriple): JsValue = JsObject(
      "triple" -> obj.triple.toJson,
      "rule" -> obj.rule.toJson,
      "predictedResult" -> obj.predictedResult.toJson
    )

    def read(json: JsValue): ResolvedPredictedTriple = {
      val selector = json.toSelector
      val res = for {
        triple <- selector("triple").to[Triple]
        rule <- selector("rule").to[ResolvedRule]
        predictedResult <- selector("predictedResult").to[PredictedResult]
      } yield {
        ResolvedPredictedTriple(triple, predictedResult, rule)
      }
      res.getOrElse(deserializationError("Unparsable resolved predicted triple."))
    }
  }

  implicit val resolvedPredictedTriplesJsonReader: RootJsonReader[IndexedSeq[ResolvedPredictedTriple]] = (json: JsValue) => {
    val selector = json.toSelector
    val res = for {
      triple <- selector("triple").to[Triple].iterator
      predictedResult <- selector("predictedResult").to[PredictedResult].iterator
      rule <- selector("rule").to[ResolvedRule].map(Iterator(_)).orElse(Some(selector("rules").toTypedIterable[ResolvedRule].iterator)).get
    } yield {
      ResolvedPredictedTriple(triple, predictedResult, rule)
    }
    res.toIndexedSeq
  }

}
