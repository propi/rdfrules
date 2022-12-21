package com.github.propi.rdfrules.utils

import scala.util.Random

trait Bootstrapper[K, V] {
  def randomIt(key: K)(builder: => IterableOnce[V]): Iterator[V]
}

object Bootstrapper {

  def apply[K, V, T](sequentially: Boolean = false, unbounded: Boolean = false)(f: Bootstrapper[K, V] => T): T = f(new ConcurrentBootstrapper[K, V](sequentially, unbounded))

  private class ConcurrentBootstrapper[K, V](sequentially: Boolean, unbounded: Boolean) extends Bootstrapper[K, V] {
    private val cache = collection.concurrent.TrieMap.empty[K, CachedBootstrapper]

    private class CachedBootstrapper(barray: Array[V]) {
      //private lazy val  = Array.from(builder)
      private var gi = 0

      private def seqRandomItem(): V = barray.synchronized {
        if (gi >= barray.length) gi = 0
        val si = Random.nextInt(barray.length - gi) + gi
        if (si != gi) {
          val temp = barray(gi)
          barray(gi) = barray(si)
          barray(si) = temp
        }
        val res = barray(gi)
        gi += 1
        res
      }

      private def randomItem(): V = barray(Random.nextInt(barray.length))

      private def boundedIt(f: () => V): Iterator[V] = new Iterator[V] {
        private var i = 0

        def hasNext: Boolean = i < barray.length

        def next(): V = {
          i += 1
          f()
        }
      }

      def seqBoundedIterator: Iterator[V] = boundedIt(seqRandomItem)

      def seqUnboundedIterator: Iterator[V] = Iterator.continually(seqRandomItem())

      def boundedIterator: Iterator[V] = boundedIt(randomItem)

      def unboundedIterator: Iterator[V] = Iterator.continually(randomItem())
    }

    private val rightIt: CachedBootstrapper => Iterator[V] = sequentially -> unbounded match {
      case (false, false) => _.boundedIterator
      case (false, true) => _.unboundedIterator
      case (true, true) => _.seqUnboundedIterator
      case (true, false) => _.seqBoundedIterator
    }

    def randomIt(key: K)(builder: => IterableOnce[V]): Iterator[V] = rightIt(cache.getOrElseUpdate(key, new CachedBootstrapper(Array.from(builder))))
  }

}