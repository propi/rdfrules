package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.TripleIndex.{HashMap, HashSet}
import com.github.propi.rdfrules.rule.TripleItemPosition

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 9. 2020.
  */
trait TripleIndex[T] {

  def getGraphs(predicate: T): HashSet[T]

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T]

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T]

  def size: Int

  def predicates: HashMap[T, PredicateIndex]

  def subjects: HashMap[T, SubjectIndex]

  def objects: HashMap[T, ObjectIndex]

  def quads: Iterator[IndexItem.Quad[T]]

  trait PredicateIndex {
    def subjects: HashMap[T, HashSet[T]]

    def objects: HashMap[T, HashSet[T]]

    def size: Int

    /**
      * (C hasCitizen ?a), or (?a isCitizenOf C)
      * For this example C is the least functional variable
      */
    final def leastFunctionalVariable: ConceptPosition = if (functionality >= inverseFunctionality) {
      TriplePosition.Object
    } else {
      TriplePosition.Subject
    }

    /**
      * (?a hasCitizen C), or (C isCitizenOf ?a)
      * For this example C is the most functional variable
      */
    def mostFunctionalVariable: ConceptPosition = if (leastFunctionalVariable == TriplePosition.Subject) {
      TriplePosition.Object
    } else {
      TriplePosition.Subject
    }

    final def functionality: Double = subjects.size.toDouble / size

    final def isFunction: Boolean = functionality == 1.0

    final def isInverseFunction: Boolean = inverseFunctionality == 1.0

    final def inverseFunctionality: Double = objects.size.toDouble / size
  }

  trait SubjectIndex {
    def predicates: HashSet[T]

    def objects: HashMap[T, HashSet[T]]

    def size: Int
  }

  trait ObjectIndex {
    def predicates: HashSet[T]

    def size: Int
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

}