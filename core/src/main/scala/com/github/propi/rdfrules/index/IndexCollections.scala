package com.github.propi.rdfrules.index

import scala.language.implicitConversions

object IndexCollections {

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

  trait MutableHashSet[T] extends HashSet[T] {
    def +=(x: T): Unit

    def -=(x: T): Unit

    def trim(): Unit
  }

  trait MutableHashMap[K, V] extends HashMap[K, V] {
    def getOrElseUpdate(key: K, default: => V): V

    def remove(key: K): Unit

    def put(key: K, value: V): Unit

    def clear(): Unit

    def trim(): Unit
  }

  trait Reflexive extends Reflexiveable {
    def hasReflexiveRecord: Boolean = true
  }

  trait NonReflexive extends Reflexiveable {
    def hasReflexiveRecord: Boolean = false
  }

  trait MutableReflexivable extends Reflexiveable {
    @volatile private var _isReflexive: Boolean = false

    final def hasReflexiveRecord: Boolean = _isReflexive

    final def setReflexivity(): Unit = _isReflexive = true
  }

  trait Builder[T] {
    def build: TripleIndex[T]
  }

  trait CollectionsBuilder {
    def emptyAnySet[A]: MutableHashSet[A]

    def emptyAnyHashMap[K, V]: MutableHashMap[K, V]
  }

  trait TypedCollectionsBuilder[T] extends CollectionsBuilder {
    def emptySet: MutableHashSet[T]

    def emptyIntHashMap: MutableHashMap[T, Int]

    def emptyHashMap[V]: MutableHashMap[T, V]

    def emptySetReflexiveable: MutableHashSet[T] with MutableReflexivable

    def emptyMapReflexiveable[V]: MutableHashMap[T, V] with MutableReflexivable

    def emptySetNonReflexive: MutableHashSet[T] with NonReflexive

    def emptySetReflexive: MutableHashSet[T] with Reflexive
  }

  private object EmptySet extends HashSet[Any] with Reflexiveable {
    def iterator: Iterator[Any] = Iterator.empty

    def contains(x: Any): Boolean = false

    def isEmpty: Boolean = true

    def hasReflexiveRecord: Boolean = false

    def size: Int = 0
  }

  def emptySet[T]: HashSet[T] with Reflexiveable = EmptySet.asInstanceOf[HashSet[T] with Reflexiveable]

  implicit def setToHashSet[T](set: Set[T]): HashSet[T] = new HashSet[T] {
    def iterator: Iterator[T] = set.iterator

    def contains(x: T): Boolean = set(x)

    def size: Int = set.size

    def isEmpty: Boolean = set.isEmpty
  }

  implicit def tripleIndexToBuilder[T](implicit tripleIndex: TripleIndex[T]): Builder[T] = new Builder[T] {
    def build: TripleIndex[T] = tripleIndex
  }

}
