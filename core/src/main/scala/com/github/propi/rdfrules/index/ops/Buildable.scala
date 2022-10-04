package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index
import com.github.propi.rdfrules.index.{TripleHashIndex, TripleIndex, TripleItemIndex}
import it.unimi.dsi.fastutil.ints.{Int2IntOpenHashMap, Int2ReferenceOpenHashMap, IntIterator, IntOpenHashSet}
import it.unimi.dsi.fastutil.objects.ObjectIterator

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait Buildable {

  implicit private def intToScalaIterator(it: IntIterator): Iterator[Int] = new Iterator[Int] {
    def hasNext: Boolean = it.hasNext

    def next(): Int = it.nextInt()
  }

  implicit private def objectToScalaIterator[T](it: ObjectIterator[T]): Iterator[T] = new Iterator[T] {
    def hasNext: Boolean = it.hasNext

    def next(): T = it.next()
  }

  private class FastMutableHashSet extends TripleHashIndex.MutableHashSet[Int] {
    private val hset = new IntOpenHashSet()

    def +=(x: Int): Unit = hset.add(x)

    def -=(x: Int): Unit = hset.remove(x)

    def iterator: Iterator[Int] = hset.iterator()

    def contains(x: Int): Boolean = hset.contains(x)

    def size: Int = hset.size()

    def trim(): Unit = hset.trim()

    def isEmpty: Boolean = hset.isEmpty
  }

  private class FastMutableHashMap[V] extends TripleHashIndex.MutableHashMap[Int, V] {
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

  private class FastIntHashMap(from: IterableOnce[(Int, Int)]) extends index.TripleIndex.HashMap[Int, Int] {
    private val hmap = new Int2IntOpenHashMap()

    for ((key, value) <- from.iterator) {
      hmap.put(key, value)
    }
    hmap.trim()

    def apply(key: Int): Int = get(key).getOrElse(throw new NoSuchElementException)

    def get(key: Int): Option[Int] = if (contains(key)) Some(hmap.get(key)) else None

    def iterator: Iterator[Int] = hmap.keySet().iterator()

    def valuesIterator: Iterator[Int] = hmap.values().iterator()

    def pairIterator: Iterator[(Int, Int)] = hmap.int2IntEntrySet().iterator().map(x => x.getIntKey -> x.getIntValue)

    def size: Int = hmap.size()

    def isEmpty: Boolean = hmap.isEmpty

    def contains(key: Int): Boolean = hmap.containsKey(key)
  }

  protected implicit val indexCollectionBuilder: TripleHashIndex.CollectionsBuilder[Int] = new TripleHashIndex.CollectionsBuilder[Int] {
    def intMap(from: IterableOnce[(Int, Int)]): TripleIndex.HashMap[Int, Int] = new FastIntHashMap(from)

    def emptySet: TripleHashIndex.MutableHashSet[Int] = new FastMutableHashSet

    def emptyHashMap[V]: TripleHashIndex.MutableHashMap[Int, V] = new FastMutableHashMap[V]

    def emptySetReflexiveable: TripleHashIndex.MutableHashSet[Int] with TripleHashIndex.MutableReflexivable = new FastMutableHashSet with TripleHashIndex.MutableReflexivable

    def emptyMapReflexiveable[V]: TripleHashIndex.MutableHashMap[Int, V] with TripleHashIndex.MutableReflexivable = new FastMutableHashMap[V] with TripleHashIndex.MutableReflexivable

    def emptySetNonReflexive: TripleHashIndex.MutableHashSet[Int] with TripleHashIndex.NonReflexive = new FastMutableHashSet with TripleHashIndex.NonReflexive

    def emptySetReflexive: TripleHashIndex.MutableHashSet[Int] with TripleHashIndex.Reflexive = new FastMutableHashSet with TripleHashIndex.Reflexive
  }

  protected def buildTripleIndex: TripleIndex[Int]

  protected def buildTripleItemIndex: TripleItemIndex

  protected def buildAll: (TripleItemIndex, TripleIndex[Int])

}