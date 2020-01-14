package com.github.propi.rdfrules.utils.extensions

import com.github.propi.rdfrules.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
object TraversableOnceExtension {

  implicit class PimpedTraversableOnce[T](val col: TraversableOnce[T]) extends AnyVal {

    def distinct: Traversable[T] = new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        val set = collection.mutable.HashSet.empty[T]
        for (x <- col) {
          if (!set(x)) {
            set += x
            f(x)
          }
        }
      }
    }

    def distinctBy[A](f: T => A): Traversable[T] = new Traversable[T] {
      def foreach[U](g: T => U): Unit = {
        val set = collection.mutable.HashSet.empty[A]
        for (x <- col) {
          val y = f(x)
          if (!set(y)) {
            set += y
            g(x)
          }
        }
      }
    }

    def concat(col2: TraversableOnce[T]): Traversable[T] = new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        col foreach f
        col2 foreach f
      }
    }

    def histogram: collection.Map[T, Int] = {
      val map = collection.mutable.HashMap.empty[T, IncrementalInt]
      col.foreach(map.getOrElseUpdate(_, IncrementalInt()).++)
      map.mapValues(_.getValue)
    }

  }

}