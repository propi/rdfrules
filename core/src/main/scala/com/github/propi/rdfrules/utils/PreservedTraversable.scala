package com.github.propi.rdfrules.utils

/**
  * Created by Vaclav Zeman on 14. 1. 2020.
  */
object PreservedTraversable {

  def apply[T](col: Traversable[T]): Traversable[T] = {
    lazy val _seq = col.toSeq
    new Traversable[T] {
      override def toSeq: Seq[T] = _seq

      def foreach[U](f: T => U): Unit = _seq.foreach(f)
    }
  }

}