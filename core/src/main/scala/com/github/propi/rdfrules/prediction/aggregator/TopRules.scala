package com.github.propi.rdfrules.prediction.aggregator

import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.RulesFactory
import com.github.propi.rdfrules.rule.{DefaultConfidence, Rule}
import com.github.propi.rdfrules.utils.TopKQueue

import com.github.propi.rdfrules.rule.Measure.ConfidenceFirstOrdering._

import scala.collection.mutable

class TopRules private(topK: Int)(implicit defaultConfidence: DefaultConfidence) extends RulesFactory {
  def newBuilder: mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] = {
    val queue = new TopKQueue[Rule.FinalRule](topK, false)(implicitly[Ordering[Rule.FinalRule]].reverse)
    new mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] {
      def clear(): Unit = queue.clear()

      def result(): Iterable[Rule.FinalRule] = LazyList.from(queue.dequeueAll)

      def addOne(elem: Rule.FinalRule): this.type = {
        queue.enqueue(elem)
        this
      }
    }
  }
}

object TopRules {
  def apply(topK: Int = -1)(implicit defaultConfidence: DefaultConfidence): TopRules = new TopRules(topK)(defaultConfidence)
}
