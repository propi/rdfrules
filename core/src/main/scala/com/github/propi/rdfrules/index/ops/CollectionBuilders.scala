package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.IndexCollections
import com.github.propi.rdfrules.index.IndexCollections.{CollectionsBuilder, MutableHashMap, MutableHashSet, MutableReflexivable, NonReflexive, Reflexive, TypedCollectionsBuilder}
import it.unimi.dsi.fastutil.ints.{Int2IntOpenHashMap, Int2ReferenceOpenHashMap, IntIterator, IntOpenHashSet}
import it.unimi.dsi.fastutil.objects.{Object2ObjectOpenHashMap, ObjectIterator, ObjectOpenHashSet}

import scala.language.implicitConversions

object CollectionBuilders {

  implicit private def intToScalaIterator(it: IntIterator): Iterator[Int] = new Iterator[Int] {
    def hasNext: Boolean = it.hasNext

    def next(): Int = it.nextInt()
  }

  implicit private def objectToScalaIterator[T](it: ObjectIterator[T]): Iterator[T] = new Iterator[T] {
    def hasNext: Boolean = it.hasNext

    def next(): T = it.next()
  }

  private class FastMutableAnySet[T] extends MutableHashSet[T] {
    private val hset = new ObjectOpenHashSet[T]()

    def +=(x: T): Unit = hset.add(x)

    def -=(x: T): Unit = hset.remove(x)

    def iterator: Iterator[T] = hset.iterator()

    def contains(x: T): Boolean = hset.contains(x)

    def size: Int = hset.size()

    def trim(): Unit = hset.trim()

    def isEmpty: Boolean = hset.isEmpty
  }

  private class FastMutableIntSet extends MutableHashSet[Int] {
    private val hset = new IntOpenHashSet()

    def +=(x: Int): Unit = hset.add(x)

    def -=(x: Int): Unit = hset.remove(x)

    def iterator: Iterator[Int] = hset.iterator()

    def contains(x: Int): Boolean = hset.contains(x)

    def size: Int = hset.size()

    def trim(): Unit = hset.trim()

    def isEmpty: Boolean = hset.isEmpty
  }

  private class FastMutableAnyMap[K, V] extends MutableHashMap[K, V] {
    private val hmap = new Object2ObjectOpenHashMap[K, V]()

    def getOrElseUpdate(key: K, default: => V): V = {
      var v = hmap.get(key)
      if (v == null) {
        v = default
        hmap.put(key, v)
      }
      v
    }

    def remove(key: K): Unit = hmap.remove(key)

    def put(key: K, value: V): Unit = hmap.put(key, value)

    def clear(): Unit = hmap.clear()

    def apply(key: K): V = {
      val v = hmap.get(key)
      if (v == null) throw new NoSuchElementException else v
    }

    def get(key: K): Option[V] = Option(hmap.get(key))

    def iterator: Iterator[K] = hmap.keySet().iterator()

    def valuesIterator: Iterator[V] = hmap.values().iterator()

    def pairIterator: Iterator[(K, V)] = hmap.object2ObjectEntrySet().iterator().map(x => x.getKey -> x.getValue)

    def size: Int = hmap.size()

    def isEmpty: Boolean = hmap.isEmpty

    def contains(key: K): Boolean = hmap.containsKey(key)

    def trim(): Unit = hmap.trim()
  }

  private class FastMutableIntMap[V] extends MutableHashMap[Int, V] {
    private val hmap = new Int2ReferenceOpenHashMap[V]()

    def getOrElseUpdate(key: Int, default: => V): V = {
      var v = hmap.get(key)
      if (v == null) {
        v = default
        hmap.put(key, v)
      }
      v
    }

    def remove(key: Int): Unit = hmap.remove(key)

    def put(key: Int, value: V): Unit = hmap.put(key, value)

    def clear(): Unit = hmap.clear()

    def apply(key: Int): V = {
      val v = hmap.get(key)
      if (v == null) throw new NoSuchElementException else v
    }

    def get(key: Int): Option[V] = Option(hmap.get(key))

    def iterator: Iterator[Int] = hmap.keySet().iterator()

    def valuesIterator: Iterator[V] = hmap.values().iterator()

    def pairIterator: Iterator[(Int, V)] = hmap.int2ReferenceEntrySet().iterator().map(x => x.getIntKey -> x.getValue)

    def size: Int = hmap.size()

    def isEmpty: Boolean = hmap.isEmpty

    def contains(key: Int): Boolean = hmap.containsKey(key)

    def trim(): Unit = hmap.trim()
  }

  private class FastMutableUnifiedIntMap extends MutableHashMap[Int, Int] {
    private val hmap = new Int2IntOpenHashMap()

    def getOrElseUpdate(key: Int, default: => Int): Int = {
      var v = hmap.get(key)
      if (v == hmap.defaultReturnValue()) {
        v = default
        hmap.put(key, v)
      }
      v
    }

    def remove(key: Int): Unit = hmap.remove(key)

    def put(key: Int, value: Int): Unit = hmap.put(key, value)

    def clear(): Unit = hmap.clear()

    def apply(key: Int): Int = {
      val v = hmap.get(key)
      if (v == hmap.defaultReturnValue()) throw new NoSuchElementException else v
    }

    def get(key: Int): Option[Int] = Option(hmap.get(key))

    def iterator: Iterator[Int] = hmap.keySet().iterator()

    def valuesIterator: Iterator[Int] = hmap.values().iterator()

    def pairIterator: Iterator[(Int, Int)] = hmap.int2IntEntrySet().iterator().map(x => x.getIntKey -> x.getIntValue)

    def size: Int = hmap.size()

    def isEmpty: Boolean = hmap.isEmpty

    def contains(key: Int): Boolean = hmap.containsKey(key)

    def trim(): Unit = hmap.trim()
  }

  implicit val intCollectionBuilder: TypedCollectionsBuilder[Int] = new TypedCollectionsBuilder[Int] {
    def emptyIntHashMap: IndexCollections.MutableHashMap[Int, Int] = new FastMutableUnifiedIntMap

    def emptySet: MutableHashSet[Int] = new FastMutableIntSet

    def emptyHashMap[V]: MutableHashMap[Int, V] = new FastMutableIntMap[V]

    def emptySetReflexiveable: MutableHashSet[Int] with MutableReflexivable = new FastMutableIntSet with MutableReflexivable

    def emptyMapReflexiveable[V]: MutableHashMap[Int, V] with MutableReflexivable = new FastMutableIntMap[V] with MutableReflexivable

    def emptySetNonReflexive: MutableHashSet[Int] with NonReflexive = new FastMutableIntSet with NonReflexive

    def emptySetReflexive: MutableHashSet[Int] with Reflexive = new FastMutableIntSet with Reflexive

    def emptyAnySet[A]: MutableHashSet[A] = new FastMutableAnySet[A]

    def emptyAnyHashMap[K, V]: MutableHashMap[K, V] = new FastMutableAnyMap[K, V]
  }

  implicit val anyCollectionBuilder: CollectionsBuilder = new CollectionsBuilder {
    def emptyAnySet[T]: MutableHashSet[T] = new FastMutableAnySet[T]

    def emptyAnyHashMap[K, V]: MutableHashMap[K, V] = new FastMutableAnyMap[K, V]
  }

}
