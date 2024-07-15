package com.github.propi.rdfrules.utils

object RichIterator {

  implicit class PimpedIterator[T](val it: Iterator[T]) extends AnyVal {
    def +[A <: T](x: A): Iterator[T] = it ++ Iterator(x)

    def :+[A <: T](x: A): Iterator[T] = this + x

    def +:[A <: T](x: A): Iterator[T] = Iterator(x) ++ it
  }

}
