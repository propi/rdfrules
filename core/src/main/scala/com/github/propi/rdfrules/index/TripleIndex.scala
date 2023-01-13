package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.IndexCollections.{Builder, HashMap, HashSet, Reflexiveable}
import com.github.propi.rdfrules.index.TripleIndex.{ObjectIndex, PredicateIndex, SubjectIndex}
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.IncrementalInt

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 9. 2020.
  */
trait TripleIndex[T] {

  main =>

  def getGraphs(predicate: T): HashSet[T]

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T]

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T]

  def size(nonReflexive: Boolean): Int

  def predicates: HashMap[T, PredicateIndex[T]]

  def subjects: HashMap[T, SubjectIndex[T]]

  def objects: HashMap[T, ObjectIndex[T]]

  def quads: Iterator[IndexItem.Quad[T]]

  def contains(s: T, p: T, o: T): Boolean = predicates.get(p).exists(_.subjects.get(s).exists(_.contains(o)))

  def contains(triple: IndexItem.Triple[T]): Boolean = contains(triple.s, triple.p, triple.o)

  def contains(quad: IndexItem.Quad[T]): Boolean = contains(quad.s, quad.p, quad.o)

  def evaluateAllLazyVals(): Unit

}

object TripleIndex {

  trait PredicateIndex[T] {
    def buildFastIntMap(from: Iterator[(T, Int)]): HashMap[T, Int]

    def context: TripleIndex[T]

    def subjects: HashMap[T, HashSet[T] with Reflexiveable]

    def objects: HashMap[T, HashSet[T] with Reflexiveable]

    def size(nonReflexive: Boolean): Int

    final lazy val (neighboursSS, neighboursSO, neighboursOO, neighboursOS) = {
      val `sp->sq` = collection.mutable.HashMap.empty[T, IncrementalInt]
      val `sp->oq` = collection.mutable.HashMap.empty[T, IncrementalInt]
      val `op->oq` = collection.mutable.HashMap.empty[T, IncrementalInt]
      val `op->sq` = collection.mutable.HashMap.empty[T, IncrementalInt]
      for ((s, objects) <- subjects.pairIterator) {
        for (pNeighbour <- context.subjects(s).predicates.iterator) {
          val pNeighbourIndex = context.predicates(pNeighbour)
          if (pNeighbourIndex == this) {
            `sp->sq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (objects.size * (objects.size - 1))
          } else {
            `sp->sq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (objects.size * pNeighbourIndex.subjects(s).size)
          }
        }
        for (pNeighbour <- context.objects.get(s).iterator.flatMap(_.predicates.iterator)) {
          `sp->oq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (objects.size * context.predicates(pNeighbour).objects(s).size)
        }
      }
      for ((o, subjects) <- objects.pairIterator) {
        for (pNeighbour <- context.objects(o).predicates.iterator) {
          val pNeighbourIndex = context.predicates(pNeighbour)
          if (pNeighbourIndex == this) {
            `op->oq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (subjects.size * (subjects.size - 1))
          } else {
            `op->oq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (subjects.size * pNeighbourIndex.objects(o).size)
          }
        }
        for (pNeighbour <- context.subjects.get(o).iterator.flatMap(_.predicates.iterator)) {
          `op->sq`.getOrElseUpdate(pNeighbour, IncrementalInt()) += (subjects.size * context.predicates(pNeighbour).subjects(o).size)
        }
      }
      (
        buildFastIntMap(`sp->sq`.view.mapValues(_.getValue).iterator),
        buildFastIntMap(`sp->oq`.view.mapValues(_.getValue).iterator),
        buildFastIntMap(`op->oq`.view.mapValues(_.getValue).iterator),
        buildFastIntMap(`op->sq`.view.mapValues(_.getValue).iterator)
      )
    }

    private def countAverageCardinality(index: HashMap[T, HashSet[T]]): Int = math.round(index.valuesIterator.map(x => x.size).sum.toFloat / index.size)

    final lazy val averageSubjectCardinality: Int = countAverageCardinality(subjects)

    final lazy val averageObjectCardinality: Int = countAverageCardinality(objects)

    /**
      * Average cardinality of higher cardinality side is suitable for qpca confidence computing
      */
    final def averageCardinality: Int = higherCardinalitySide match {
      case TriplePosition.Subject => averageSubjectCardinality
      case TriplePosition.Object => averageObjectCardinality
    }

    /**
      * Mode probability is needed for lift measure calculation
      */
    final lazy val modeProbability: Double = lowerCardinalitySide match {
      case TriplePosition.Subject => subjects.valuesIterator.map(_.size).max.toDouble / size(false)
      case TriplePosition.Object => objects.valuesIterator.map(_.size).max.toDouble / size(false)
    }

    final lazy val pcaNegatives: Int = higherCardinalitySide match {
      case TriplePosition.Subject =>
        val valuesCount = objects.size
        subjects.valuesIterator.map(x => valuesCount - x.size).sum
      case TriplePosition.Object =>
        val valuesCount = subjects.size
        objects.valuesIterator.map(x => valuesCount - x.size).sum
    }

    final lazy val pcaNonReflexiveNegatives: Int = higherCardinalitySide match {
      case TriplePosition.Subject =>
        val valuesCount = objects.size
        subjects.valuesIterator.map(x => valuesCount - x.size(true)).sum
      case TriplePosition.Object =>
        val valuesCount = subjects.size
        objects.valuesIterator.map(x => valuesCount - x.size(true)).sum
    }

    final lazy val subjectRelativeCardinality: Double = subjects.size.toDouble / size(false)

    final lazy val objectRelativeCardinality: Double = objects.size.toDouble / size(false)

    /**
      * (C hasCitizen ?a), or (?a isCitizenOf C)
      * For this example C is the least functional variable
      */
    final lazy val lowerCardinalitySide: ConceptPosition = if (subjectRelativeCardinality >= objectRelativeCardinality) {
      TriplePosition.Object
    } else {
      TriplePosition.Subject
    }

    /**
      * (?a hasCitizen C), or (C isCitizenOf ?a)
      * For this example C is the most functional variable
      */
    final lazy val higherCardinalitySide: ConceptPosition = if (lowerCardinalitySide == TriplePosition.Subject) {
      TriplePosition.Object
    } else {
      TriplePosition.Subject
    }

    final def isFunction: Boolean = subjectRelativeCardinality == 1.0

    final def isInverseFunction: Boolean = objectRelativeCardinality == 1.0
  }

  trait SubjectIndex[T] {
    def predicates: HashSet[T]

    def objects: HashMap[T, HashSet[T] with Reflexiveable]

    def size(nonReflexive: Boolean): Int
  }

  trait ObjectIndex[T] {
    def predicates: HashSet[T]

    def size(nonReflexive: Boolean): Int
  }

  implicit def builderToTripleIndex[T](implicit builder: Builder[T]): TripleIndex[T] = builder.build

}