package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.IndexCollections.{HashSet, Reflexiveable}
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{IndexCollections, TripleIndex}
import com.github.propi.rdfrules.rule.TripleItemPosition

case class PredictionTask(p: Int, c: TripleItemPosition[Int]) {
  def predictionTaskPattern: PredictionTaskPattern = PredictionTaskPattern(p, c match {
    case _: TripleItemPosition.Subject[_] => TriplePosition.Object
    case _: TripleItemPosition.Object[_] => TriplePosition.Subject
  })

  def index(implicit tripleIndex: TripleIndex[Int]): HashSet[Int] with Reflexiveable = c match {
    case TripleItemPosition.Subject(s) => tripleIndex.predicates.get(p).flatMap(_.subjects.get(s)).getOrElse(IndexCollections.emptySet[Int])
    case TripleItemPosition.Object(o) => tripleIndex.predicates.get(p).flatMap(_.objects.get(o)).getOrElse(IndexCollections.emptySet[Int])
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

  def apply(triple: IntTriple, targetVariable: ConceptPosition): PredictionTask = targetVariable match {
    case TriplePosition.Subject => PredictionTask(triple.p, TripleItemPosition.Object(triple.o))
    case TriplePosition.Object => PredictionTask(triple.p, TripleItemPosition.Subject(triple.o))
  }
}