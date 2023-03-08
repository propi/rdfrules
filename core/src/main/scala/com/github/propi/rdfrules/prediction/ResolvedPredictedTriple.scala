package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.utils.JsonSelector.PimpedJsValue
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
sealed trait ResolvedPredictedTriple {
  def triple: Triple

  def rules: Iterable[ResolvedRule]

  def predictedResult: PredictedResult

  def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple

  def score: Double

  override def toString: String = s"$triple, score: $score\n - ${rules.mkString("\n - ")}"
}

object ResolvedPredictedTriple {

  sealed trait Single extends ResolvedPredictedTriple {
    def rule: ResolvedRule

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Single
  }

  sealed trait Grouped extends ResolvedPredictedTriple {
    def score: Double

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Grouped
  }

  private case class Basic(triple: Triple, rule: ResolvedRule)(val predictedResult: PredictedResult) extends Single {
    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Single = PredictedTriple(
      triple.toIndexedTriple,
      predictedResult,
      rule.toRule
    )

    def rules: Iterable[ResolvedRule] = List(rule)

    def score: Double = 0.0
  }

  private case class BasicGrouped(triple: Triple)(val rules: Iterable[ResolvedRule], val predictedResult: PredictedResult, val score: Double) extends Grouped {
    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Grouped = PredictedTriple(
      triple.toIndexedTriple,
      predictedResult,
      rules.map(_.toRule),
      score
    )
  }

  implicit private def tripleToResolvedTriple(triple: IntTriple)(implicit mapper: TripleItemIndex): Triple = Triple(
    mapper.getTripleItem(triple.s).asInstanceOf[TripleItem.Uri],
    mapper.getTripleItem(triple.p).asInstanceOf[TripleItem.Uri],
    mapper.getTripleItem(triple.o)
  )

  def apply(triple: Triple, predictedResult: PredictedResult, rule: ResolvedRule): ResolvedPredictedTriple.Single = Basic(triple, rule)(predictedResult)

  def apply(triple: Triple, predictedResult: PredictedResult, rules: Iterable[ResolvedRule], score: Double): ResolvedPredictedTriple = BasicGrouped(triple)(rules.toVector, predictedResult, score)

  def apply(predictedTriple: PredictedTriple.Single)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple.Single = apply(
    predictedTriple.triple,
    predictedTriple.predictedResult,
    ResolvedRule(predictedTriple.rule)
  )

  def apply(predictedTriple: PredictedTriple.Grouped)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple.Grouped = BasicGrouped(
    predictedTriple.triple
  )(predictedTriple.rules.iterator.map(ResolvedRule(_)).toVector, predictedTriple.predictedResult, predictedTriple.score)

  def apply(predictedTriple: PredictedTriple)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple = predictedTriple match {
    case x: PredictedTriple.Grouped => apply(x)
    case x: PredictedTriple.Single => apply(x)
  }

  implicit val resolvedPredictedTripleJsonFormat: RootJsonFormat[ResolvedPredictedTriple] = new RootJsonFormat[ResolvedPredictedTriple] {
    def write(obj: ResolvedPredictedTriple): JsValue = {
      val fields = Map(
        "triple" -> obj.triple.toJson,
        "rules" -> obj.rules.toJson,
        "predictedResult" -> obj.predictedResult.toJson
      )
      obj match {
        case x: ResolvedPredictedTriple.Grouped => JsObject(fields + ("score" -> x.score.toJson))
        case _ => JsObject(fields)
      }
    }

    def read(json: JsValue): ResolvedPredictedTriple = {
      val selector = json.toSelector
      val res = for {
        triple <- selector.get("triple").toOpt[Triple]
        rules <- selector.get("rules").toOpt[Vector[ResolvedRule]]
        predictedResult <- selector.get("predictedResult").toOpt[PredictedResult]
      } yield {
        (rules, selector.get("score").toOpt[Double]) match {
          case (rules, Some(score)) => ResolvedPredictedTriple(triple, predictedResult, rules, score)
          case (Seq(rule), None) => ResolvedPredictedTriple(triple, predictedResult, rule)
          case _ => ResolvedPredictedTriple(triple, predictedResult, rules, 0.0)
        }
      }
      res.getOrElse(deserializationError("Unparsable resolved predicted triple."))
    }
  }

}
