package com.github.propi.rdfrules.utils.extensions

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
object IterableOnceExtension {

  implicit class PimpedIterableOnce[T](val col: IterableOnce[T]) extends AnyVal {
    def distinct: Iterable[T] = new Iterable[T] {
      def iterator: Iterator[T] = col.iterator.distinct
    }

    def distinctBy[A](f: T => A): Iterable[T] = new Iterable[T] {
      def iterator: Iterator[T] = col.iterator.distinctBy(f)
    }
  }

}