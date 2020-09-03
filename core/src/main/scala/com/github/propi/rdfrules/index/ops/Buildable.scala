package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{TripleHashIndex, TripleItemHashIndex}
import it.unimi.dsi.fastutil.ints.{Int2ReferenceOpenHashMap, IntOpenHashSet}

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait Buildable {

  protected implicit val indexCollectionBuilder: TripleHashIndex.CollectionsBuilder[Int] = new TripleHashIndex.CollectionsBuilder[Int] {
    def emptySet: TripleHashIndex.MutableHashSet[Int] = new TripleHashIndex.MutableHashSet[Int] {
      private val hset = new IntOpenHashSet()

      def +=(x: Int): Unit = hset.add(x)

      def -=(x: Int): Unit = hset.remove(x)

      def iterator: Iterator[Int] = hset.iterator().asScala.asInstanceOf[Iterator[Int]]

      def apply(x: Int): Boolean = hset.contains(x)

      def size: Int = hset.size()

      def trim(): Unit = hset.trim()

      def isEmpty: Boolean = hset.isEmpty
    }

    def emptyHashMap[V]: TripleHashIndex.MutableHashMap[Int, V] = new TripleHashIndex.MutableHashMap[Int, V] {
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

      def keySet: TripleHashIndex.HashSet[Int] = new TripleHashIndex.HashSet[Int] {
        private val hset: java.util.Set[Integer] = hmap.keySet()

        def iterator: Iterator[Int] = hset.iterator().asScala.asInstanceOf[Iterator[Int]]

        def apply(x: Int): Boolean = hset.contains(x)

        def size: Int = hset.size()

        def trim(): Unit = hset match {
          case x: IntOpenHashSet => x.trim()
          case _ =>
        }

        def isEmpty: Boolean = hset.isEmpty
      }

      def get(key: Int): Option[V] = Option(hmap.get(key))

      def keysIterator: Iterator[Int] = hmap.keySet().iterator().asScala.asInstanceOf[Iterator[Int]]

      def valuesIterator: Iterator[V] = hmap.values().iterator().asScala

      def iterator: Iterator[(Int, V)] = hmap.int2ReferenceEntrySet().iterator().asScala.map(x => x.getIntKey -> x.getValue)

      def size: Int = hmap.size()

      def isEmpty: Boolean = hmap.isEmpty

      def contains(key: Int): Boolean = hmap.containsKey(key)

      def trim(): Unit = hmap.trim()
    }
  }

  protected def buildTripleHashIndex: TripleHashIndex[Int]

  protected def buildTripleItemHashIndex: TripleItemHashIndex

  protected def buildAll: (TripleItemHashIndex, TripleHashIndex[Int])

}
