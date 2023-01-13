package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.IndexCollections.ReflexivableHashSet
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{IndexCollections, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.TripleItemPosition

case class PredictionTask(p: Int, c: TripleItemPosition[Int]) {
  def targetVariable: ConceptPosition = c match {
    case _: TripleItemPosition.Subject[_] => TriplePosition.Object
    case _: TripleItemPosition.Object[_] => TriplePosition.Subject
  }

  def predictionTaskPattern: PredictionTaskPattern = PredictionTaskPattern(p, targetVariable)

  def index(implicit tripleIndex: TripleIndex[Int]): ReflexivableHashSet[Int] = c match {
    case TripleItemPosition.Subject(s) => new ReflexivableHashSet[Int](s, tripleIndex.predicates.get(p).flatMap(_.subjects.get(s)).getOrElse(IndexCollections.emptySet[Int]))
    case TripleItemPosition.Object(o) => new ReflexivableHashSet[Int](o, tripleIndex.predicates.get(p).flatMap(_.objects.get(o)).getOrElse(IndexCollections.emptySet[Int]))
  }
}

object PredictionTask {
  case class Resolved(p: TripleItem.Uri, c: TripleItemPosition[TripleItem])

  object Resolved {
    def apply(predictionTask: PredictionTask)(implicit mapper: TripleItemIndex): Resolved = Resolved(
      mapper.getTripleItem(predictionTask.p).asInstanceOf[TripleItem.Uri],
      predictionTask.c.map(mapper.getTripleItem)
    )
  }

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