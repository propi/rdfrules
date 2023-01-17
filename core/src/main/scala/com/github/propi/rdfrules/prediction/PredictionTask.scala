package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.IndexCollections.ReflexivableHashSet
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{IndexCollections, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.serialization.TripleItemSerialization._

import java.io.ByteArrayInputStream

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
  case class Resolved(p: TripleItem.Uri, c: TripleItemPosition[TripleItem]) {
    override def toString: String = c match {
      case TripleItemPosition.Subject(s) => s"($s $p ?)"
      case TripleItemPosition.Object(o) => s"(? $p $o)"
    }
  }

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

  implicit def predictionTaskSerializer(implicit mapper: TripleItemIndex): Serializer[PredictionTask] = (v: PredictionTask) => {
    val resolved = Resolved(v)
    resolved.c match {
      case TripleItemPosition.Subject(s) => Serializer.serialize((1: Byte, s, resolved.p))
      case TripleItemPosition.Object(o) => Serializer.serialize((2: Byte, o, resolved.p))
    }
  }

  implicit def predictionTaskDeserializer(implicit mapper: TripleItemIndex): Deserializer[PredictionTask] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (t, c, p) = Deserializer.deserialize[(Byte, TripleItem, TripleItem.Uri)](bais)
    if (t == 1) {
      PredictionTask(mapper.getIndex(p), TripleItemPosition.Subject(mapper.getIndex(c)))
    } else {
      PredictionTask(mapper.getIndex(p), TripleItemPosition.Object(mapper.getIndex(c)))
    }
  }
}