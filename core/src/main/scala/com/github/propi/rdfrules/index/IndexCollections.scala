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

    def size(nonReflexive: Boolean): Int = if (nonReflexive && hasReflexiveRecord) size - 1 else size
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

  class ReflexivableHashSet[T](value: T, hset: HashSet[T] with Reflexiveable) {
    def iterator(nonReflexive: Boolean): Iterator[T] = if (nonReflexive) hset.iterator.filter(_ != value) else hset.iterator

    def contains(x: T, nonReflexive: Boolean): Boolean = if (nonReflexive) value != x && hset.contains(x) else hset.contains(x)

    def size(nonReflexive: Boolean): Int = hset.size(nonReflexive)

    def isEmpty(nonReflexive: Boolean): Boolean = hset.size(nonReflexive) == 0

    def hasReflexiveRecord: Boolean = hset.hasReflexiveRecord
  }

  private object EmptySet extends HashSet[Any] with Reflexiveable {
    def iterator: Iterator[Any] = Iterator.empty

    def contains(x: Any): Boolean = false

    def isEmpty: Boolean = true

    def hasReflexiveRecord: Boolean = false

    def size: Int = 0
  }

  def emptySet[T]: HashSet[T] with Reflexiveable = EmptySet.asInstanceOf[HashSet[T] with Reflexiveable]

  class MergedHashSet[T](hset1: HashSet[T], hset2: HashSet[T]) extends HashSet[T] {
    def iterator: Iterator[T] = hset1.iterator ++ hset2.iterator.filter(!hset1.contains(_))

    def contains(x: T): Boolean = hset1.contains(x) || hset2.contains(x)

    lazy val _size: Int = hset1.size + hset2.iterator.count(!hset1.contains(_))

    def size: Int = _size

    def isEmpty: Boolean = hset1.isEmpty && hset2.isEmpty
  }

  class MergedHashMap[K, +V](hmap1: HashMap[K, V], hmap2: HashMap[K, V])(merge: K => (V, V) => V) extends MergedHashSet(hmap1, hmap2) with HashMap[K, V] {
    def apply(key: K): V = hmap1.get(key) -> hmap2.get(key) match {
      case (Some(x), Some(y)) => merge(key)(x, y)
      case (Some(x), None) => x
      case (None, Some(y)) => y
      case (None, None) => None.get
    }

    def get(key: K): Option[V] = hmap1.get(key) -> hmap2.get(key) match {
      case (Some(x), Some(y)) => Some(merge(key)(x, y))
      case (x: Some[V], None) => x
      case (None, y: Some[V]) => y
      case (None, None) => None
    }

    def valuesIterator: Iterator[V] = pairIterator.map(_._2)

    def pairIterator: Iterator[(K, V)] = hmap1.pairIterator.map(x => x._1 -> hmap2.get(x._1).map(merge(x._1)(x._2, _)).getOrElse(x._2)) ++
      hmap2.pairIterator.filter(x => !hmap1.contains(x._1))
  }

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
