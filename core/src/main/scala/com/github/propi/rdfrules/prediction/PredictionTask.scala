package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule.TripleItemPosition

case class PredictionTask(p: Int, c: TripleItemPosition[Int]) {
  def index(implicit tripleIndex: TripleIndex[Int]): TripleIndex.HashSet[Int] with TripleIndex.Reflexiveable = c match {
    case TripleItemPosition.Subject(s) => tripleIndex.predicates.get(p).flatMap(_.subjects.get(s)).getOrElse(TripleIndex.emptySet[Int])
    case TripleItemPosition.Object(o) => tripleIndex.predicates.get(p).flatMap(_.objects.get(o)).getOrElse(TripleIndex.emptySet[Int])
  }
}

object PredictionTask {
  def apply(triple: IntTriple)(implicit tripleIndex: TripleIndex[Int]): PredictionTask = tripleIndex.predicates.get(triple.p) match {
    case Some(pindex) => pindex.higherCardinalitySide match {
      case TriplePosition.Subject => PredictionTask(triple.p, TripleItemPosition.Subject(triple.s))
      case TriplePosition.Object => PredictionTask(triple.p, TripleItemPosition.Object(triple.o))
    }
    case None => PredictionTask(triple.p, TripleItemPosition.Subject(triple.s))
  }

  def apply(predictedTriple: PredictedTriple)(implicit tripleIndex: TripleIndex[Int]): PredictionTask = apply(predictedTriple.triple)
}