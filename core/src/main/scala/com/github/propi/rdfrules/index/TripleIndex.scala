package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.TripleIndex.{HashMap, HashSet, Reflexiveable}
import com.github.propi.rdfrules.rule.TripleItemPosition

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 9. 2020.
  */
trait TripleIndex[T] {

  def getGraphs(predicate: T): HashSet[T]

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T]

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T]

  def size(nonReflexive: Boolean): Int

  def predicates: HashMap[T, PredicateIndex]

  def subjects: HashMap[T, SubjectIndex]

  def objects: HashMap[T, ObjectIndex]

  def quads: Iterator[IndexItem.Quad[T]]

  def evaluateAllLazyVals(): Unit

  trait PredicateIndex {
    def subjects: HashMap[T, HashSet[T] with Reflexiveable]

    def objects: HashMap[T, HashSet[T] with Reflexiveable]

    def size(nonReflexive: Boolean): Int

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

  trait SubjectIndex {
    def predicates: HashSet[T]

    def objects: HashMap[T, HashSet[T] with Reflexiveable]

    def size(nonReflexive: Boolean): Int
  }

  trait ObjectIndex {
    def predicates: HashSet[T]

    def size(nonReflexive: Boolean): Int
  }

}

object TripleIndex {

  trait HashSet[T] {
    def iterator: Iterator[T]

    def contains(x: T): Boolean

    def size: Int

    def isEmpty: Boolean
  }

  trait HashMap[K, +V] extends HashSet[K] {
    def apply(key: K): V

    def get(key: K): Option[V]

    def valuesIterator: Iterator[V]

    def pairIterator: Iterator[(K, V)]
  }

  trait Reflexiveable {
    def hasReflexiveRecord: Boolean

    def size: Int

    final def size(nonReflexive: Boolean): Int = if (nonReflexive && hasReflexiveRecord) size - 1 else size
  }

  trait Builder[T] {
    def build: TripleIndex[T]
  }

  implicit def setToHashSet[T](set: Set[T]): HashSet[T] = new HashSet[T] {
    def iterator: Iterator[T] = set.iterator

    def contains(x: T): Boolean = set(x)

    def size: Int = set.size

    def isEmpty: Boolean = set.isEmpty
  }

  implicit def builderToTripleIndex[T](implicit builder: Builder[T]): TripleIndex[T] = builder.build

  implicit def indexToBuilder(index: Index): Builder[Int] = new Builder[Int] {
    def build: TripleIndex[Int] = index.tripleMap
  }

  implicit def tripleIndexToBuilder[T](implicit tripleIndex: TripleIndex[T]): Builder[T] = new Builder[T] {
    def build: TripleIndex[T] = tripleIndex
  }

}