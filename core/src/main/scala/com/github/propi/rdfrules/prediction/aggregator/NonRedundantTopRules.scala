package com.github.propi.rdfrules.prediction.aggregator

import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.RulesFactory
import com.github.propi.rdfrules.rule.Measure.ConfidenceFirstOrdering._
import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure, Rule}
import com.github.propi.rdfrules.utils.TopKQueue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class NonRedundantTopRules private(topK: Int)(implicit defaultConfidence: DefaultConfidence) extends RulesFactory {
  def newBuilder: mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] = {
    val clusters = collection.mutable.HashMap.empty[Int, Rule.FinalRule]
    val rulesOrdering = implicitly[Ordering[Rule.FinalRule]].reverse
    new mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] {
      def clear(): Unit = clusters.clear()

      def result(): Iterable[Rule.FinalRule] = {
        val queue = new TopKQueue[Rule.FinalRule](topK, false)(rulesOrdering.reverse)
        clusters.valuesIterator.foreach(queue.enqueue)
        val res = ListBuffer.empty[Rule.FinalRule]
        queue.dequeueAll.foreach(res.prepend)
        res
      }

      def addOne(elem: Rule.FinalRule): this.type = {
        val cluster = elem.measures.get[Measure.Cluster].map(_.number).getOrElse(-1)
        clusters.updateWith(cluster) {
          case Some(ruleMax) => if (rulesOrdering.gt(elem, ruleMax)) Some(elem) else Some(ruleMax)
          case None => Some(elem)
        }
        this
      }
    }
  }
}

object NonRedundantTopRules {
  def apply(topK: Int = -1)(implicit defaultConfidence: DefaultConfidence): NonRedundantTopRules = new NonRedundantTopRules(topK)(defaultConfidence)
}