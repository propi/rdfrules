package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.rule.Rule.FinalRule

import scala.collection.mutable

class PredictedTriplesAggregator private(scoreBuilder: collection.mutable.Builder[FinalRule, Double],
                                         rulesBuilder: collection.mutable.Builder[FinalRule, Iterable[FinalRule]])
  extends collection.mutable.Builder[PredictedTriple, PredictedTriple.Grouped] {

  private var first = Option.empty[PredictedTriple.Single]

  def clear(): Unit = {
    scoreBuilder.clear()
    rulesBuilder.clear()
    first = None
  }

  def result(): PredictedTriple.Grouped = {
    val pt = first.get
    first = None
    PredictedTriple(pt.triple, pt.predictedResult, rulesBuilder.result(), scoreBuilder.result())
  }

  def addOne(elem: PredictedTriple): PredictedTriplesAggregator.this.type = {
    if (first.isEmpty) first = Some(elem.toSinglePredictedTriples.next())
    elem match {
      case x: PredictedTriple.Single =>
        scoreBuilder.addOne(x.rule)
        rulesBuilder.addOne(x.rule)
      case _: PredictedTriple.Grouped => for (rule <- elem.rules) {
        scoreBuilder.addOne(rule)
        rulesBuilder.addOne(rule)
      }
    }
    this
  }
}

object PredictedTriplesAggregator {

  sealed trait FromSpecific[-A, +C] extends collection.Factory[A, C] {
    final def fromSpecific(it: IterableOnce[A]): C = it.iterator.foldLeft(newBuilder)(_.addOne(_)).result()
  }

  trait ScoreFactory extends FromSpecific[FinalRule, Double]

  trait RulesFactory extends FromSpecific[FinalRule, Iterable[FinalRule]]

  object EmptyScoreFactory extends ScoreFactory {
    def newBuilder: mutable.Builder[FinalRule, Double] = new mutable.Builder[FinalRule, Double] {
      def clear(): Unit = ()

      def result(): Double = 0.0

      def addOne(elem: FinalRule): this.type = this
    }
  }

  object EmptyRulesFactory extends RulesFactory {
    def newBuilder: mutable.Builder[FinalRule, Iterable[FinalRule]] = {
      val buffer = collection.mutable.ArrayBuffer.empty[FinalRule]
      new mutable.Builder[FinalRule, Iterable[FinalRule]] {
        def clear(): Unit = buffer.clear()

        def result(): Iterable[FinalRule] = buffer

        def addOne(elem: FinalRule): this.type = {
          buffer.addOne(elem)
          this
        }
      }
    }
  }

  def apply(scoreFactory: ScoreFactory, rulesFactory: RulesFactory): FromSpecific[PredictedTriple, PredictedTriple.Grouped] = new FromSpecific[PredictedTriple, PredictedTriple.Grouped] {
    def newBuilder: mutable.Builder[PredictedTriple, PredictedTriple.Grouped] = new PredictedTriplesAggregator(scoreFactory.newBuilder, rulesFactory.newBuilder)
  }

}