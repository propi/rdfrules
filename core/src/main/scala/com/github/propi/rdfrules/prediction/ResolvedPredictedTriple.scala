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

  def :+(rule: ResolvedRule): ResolvedPredictedTriple

  def :++(rules: Iterable[ResolvedRule]): ResolvedPredictedTriple
}

object ResolvedPredictedTriple {

  sealed trait Single extends ResolvedPredictedTriple {
    def rule: ResolvedRule

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Single
  }

  sealed trait Scored extends ResolvedPredictedTriple {
    def score: Double

    def :+(rule: ResolvedRule): Scored

    def :++(rules: Iterable[ResolvedRule]): Scored

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Scored
  }

  private case class Basic(triple: Triple, rule: ResolvedRule)(val predictedResult: PredictedResult) extends Single {
    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Single = PredictedTriple(
      triple.toIndexedTriple,
      predictedResult,
      rule.toRule
    )


    def rules: Iterable[ResolvedRule] = List(rule)

    def :+(rule: ResolvedRule): ResolvedPredictedTriple = BasicGrouped(triple)(Vector(this.rule, rule), predictedResult)

    def :++(rules: Iterable[ResolvedRule]): ResolvedPredictedTriple = BasicGrouped(triple)(Vector.from(Iterator(rule) ++ rules.iterator), predictedResult)
  }

  private case class ScoredBasicGrouped(triple: Triple)(val rules: Vector[ResolvedRule], val predictedResult: PredictedResult, val score: Double) extends Scored {
    def :+(rule: ResolvedRule): Scored = copy()(this.rules :+ rule, predictedResult, score)

    def :++(rules: Iterable[ResolvedRule]): Scored = copy()(this.rules :++ rules, predictedResult, score)

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple.Scored = {
      val headRule = rules.head
      (PredictedTriple(
        triple.toIndexedTriple,
        predictedResult,
        headRule.toRule
      ) :++ rules.tail.view.map(_.toRule)).withScore(score)
    }
  }

  private case class BasicGrouped(triple: Triple)(val rules: Vector[ResolvedRule], val predictedResult: PredictedResult) extends ResolvedPredictedTriple {
    def :+(rule: ResolvedRule): ResolvedPredictedTriple = copy()(this.rules :+ rule, predictedResult)

    def :++(rules: Iterable[ResolvedRule]): ResolvedPredictedTriple = copy()(this.rules :++ rules, predictedResult)

    def toPredictedTriple(implicit tripleItemIndex: TripleItemIndex): PredictedTriple = {
      val headRule = rules.head
      PredictedTriple(
        triple.toIndexedTriple,
        predictedResult,
        headRule.toRule
      ) :++ rules.tail.view.map(_.toRule)
    }
  }

  implicit private def tripleToResolvedTriple(triple: IntTriple)(implicit mapper: TripleItemIndex): Triple = Triple(
    mapper.getTripleItem(triple.s).asInstanceOf[TripleItem.Uri],
    mapper.getTripleItem(triple.p).asInstanceOf[TripleItem.Uri],
    mapper.getTripleItem(triple.o)
  )

  def apply(triple: Triple, predictedResult: PredictedResult, rule: ResolvedRule): ResolvedPredictedTriple.Single = Basic(triple, rule)(predictedResult)

  def apply(triple: Triple, predictedResult: PredictedResult, rules: Iterable[ResolvedRule]): ResolvedPredictedTriple = BasicGrouped(triple)(rules.toVector, predictedResult)

  def apply(predictedTriple: PredictedTriple.Single)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple.Single = apply(
    predictedTriple.triple,
    predictedTriple.predictedResult,
    ResolvedRule(predictedTriple.rule)
  )

  def apply(predictedTriple: PredictedTriple.Scored)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple.Scored = ScoredBasicGrouped(
    predictedTriple.triple
  )(predictedTriple.rules.iterator.map(ResolvedRule(_)).toVector, predictedTriple.predictedResult, predictedTriple.score)

  def apply(predictedTriple: PredictedTriple)(implicit mapper: TripleItemIndex): ResolvedPredictedTriple = {
    predictedTriple match {
      case x: PredictedTriple.Scored => apply(x)
      case x: PredictedTriple.Single => apply(x)
      case _ => BasicGrouped(
        predictedTriple.triple
      )(predictedTriple.rules.iterator.map(ResolvedRule(_)).toVector, predictedTriple.predictedResult)
    }
  }

  implicit val resolvedPredictedTripleJsonFormat: RootJsonFormat[ResolvedPredictedTriple] = new RootJsonFormat[ResolvedPredictedTriple] {
    def write(obj: ResolvedPredictedTriple): JsValue = {
      val fields = Map(
        "triple" -> obj.triple.toJson,
        "rules" -> obj.rules.toJson,
        "predictedResult" -> obj.predictedResult.toJson
      )
      obj match {
        case x: ResolvedPredictedTriple.Scored => JsObject(fields + ("score" -> x.score.toJson))
        case _ => JsObject(fields)
      }
    }

    def read(json: JsValue): ResolvedPredictedTriple = {
      val selector = json.toSelector
      val res = for {
        triple <- selector("triple").to[Triple]
        rules <- selector("rules").to[Vector[ResolvedRule]]
        predictedResult <- selector("predictedResult").to[PredictedResult]
      } yield {
        val resolvedPredictedTriple = rules match {
          case Seq(head) => ResolvedPredictedTriple(triple, predictedResult, head)
          case _ => ResolvedPredictedTriple(triple, predictedResult, rules)
        }
        selector("score").to[Double].map(score => resolvedPredictedTriple.withScore(score)).getOrElse(resolvedPredictedTriple)
      }
      res.getOrElse(deserializationError("Unparsable resolved predicted triple."))
    }
  }

  implicit class PimpedResolvedPredictedTriple(val resolvedPredictedTriple: ResolvedPredictedTriple) extends AnyVal {
    def withScore(score: Double): Scored = ScoredBasicGrouped(resolvedPredictedTriple.triple)(resolvedPredictedTriple.rules.toVector, resolvedPredictedTriple.predictedResult, score)
  }

  /*implicit val resolvedPredictedTriplesJsonReader: RootJsonReader[IndexedSeq[ResolvedPredictedTriple]] = (json: JsValue) => {
    val selector = json.toSelector
    val res = for {
      triple <- selector("triple").to[Triple].iterator
      predictedResult <- selector("predictedResult").to[PredictedResult].iterator
      rule <- selector("rule").to[ResolvedRule].map(Iterator(_)).orElse(Some(selector("rules").toTypedIterable[ResolvedRule].iterator)).get
    } yield {
      ResolvedPredictedTriple(triple, predictedResult, rule)
    }
    res.toIndexedSeq
  }*/

}
