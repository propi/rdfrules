package com.github.propi.rdfrules.utils

import com.github.propi.rdfrules.utils.ForEach.KnownSizeForEach

import scala.collection.immutable.ArraySeq
import scala.collection.{Factory, MapView}
import scala.language.implicitConversions
import scala.reflect.ClassTag

trait ForEach[+T] {
  self =>

  def foreach(f: T => Unit): Unit

  def knownSize: Int = -1

  def size: Int = if (knownSize >= 0) {
    knownSize
  } else {
    var i = 0
    foreach(_ => i += 1)
    i
  }

  def cached[A >: T](implicit tag: ClassTag[A]): ForEach[A] = {
    lazy val col = self.to(Array)
    new ForEach[A] {
      override def knownSize: Int = col.knownSize

      def foreach(f: A => Unit): Unit = col.foreach(f)

      override def toIndexedSeq: IndexedSeq[A] = ArraySeq.unsafeWrapArray(col)

      override def toSeq: Seq[A] = toIndexedSeq

      override def toArray[B >: A](implicit tag: ClassTag[B]): Array[B] = col.asInstanceOf[Array[B]]
    }
  }

  def isEmpty: Boolean = headOption.isEmpty

  def lastOption: Option[T] = {
    val lastValue = MutableOption.empty[T]
    foreach(x => lastValue.set(x))
    lastValue.toOption
  }

  def headOption: Option[T] = {
    foreach(x => return Some(x))
    None
  }

  def distinct: ForEach[T] = (f: T => Unit) => {
    val set = collection.mutable.HashSet.empty[T]
    self.foreach { x =>
      if (!set(x)) {
        set += x
        f(x)
      }
    }
  }

  def distinctBy[A](f: T => A): ForEach[T] = (g: T => Unit) => {
    val set = collection.mutable.HashSet.empty[A]
    self.foreach { x =>
      val y = f(x)
      if (!set(y)) {
        set += y
        g(x)
      }
    }
  }

  def takeWhile(p: T => Boolean): ForEach[T] = new ForEach[T] {
    def foreach(f: T => Unit): Unit = {
      for (x <- self) {
        if (p(x)) {
          f(x)
        } else {
          return
        }
      }
    }
  }

  def take(n: Int): ForEach[T] = {
    if (n >= 0) {
      val col = new ForEach[T] {
        def foreach(f: T => Unit): Unit = {
          var i = 0
          self.foreach { x =>
            i += 1
            if (i <= n) f(x)
            if (i == n) return
          }
        }
      }
      if (knownSize >= 0) {
        new KnownSizeForEach(math.max(math.min(knownSize, n), 0), col)
      } else {
        col
      }
    } else {
      this
    }
  }

  def drop(n: Int): ForEach[T] = {
    if (n >= 0) {
      val col = new ForEach[T] {
        def foreach(f: T => Unit): Unit = {
          var i = 0
          self.foreach { x =>
            i += 1
            if (i > n) f(x)
          }
        }
      }
      if (knownSize >= 0) {
        new KnownSizeForEach(math.max(0, knownSize - n), col)
      } else {
        col
      }
    } else {
      this
    }
  }

  def head: T = headOption.get

  def last: T = lastOption.get

  def slice(from: Int, until: Int): ForEach[T] = drop(from).take(until - from)

  def groupBy[K, C](g: T => K)(factory: Factory[T, C]): Map[K, C] = {
    val hmap = collection.mutable.Map.empty[K, collection.mutable.Builder[T, C]]
    self.foreach(x => hmap.getOrElseUpdate(g(x), factory.newBuilder).addOne(x))
    hmap.view.mapValues(_.result()).toMap
  }

  def concat[A >: T](that: ForEach[A]): ForEach[A] = {
    val col = new ForEach[A] {
      def foreach(f: A => Unit): Unit = {
        self.foreach(f)
        that.foreach(f)
      }
    }
    if (knownSize >= 0 && that.knownSize >= 0) {
      new KnownSizeForEach(knownSize + that.knownSize, col)
    } else {
      col
    }
  }

  def foldLeft[A](a: A)(f: (A, T) => A): A = {
    var res = a
    self.foreach(x => res = f(res, x))
    res
  }

  def flatMap[A](g: T => ForEach[A]): ForEach[A] = (f: A => Unit) => self.foreach(g(_).foreach(f))

  def reduce[A >: T](f: (A, A) => A): A = reduceOption(f).get

  def reduceOption[A >: T](f: (A, A) => A): Option[A] = foldLeft(Option.empty[A])((x, y) => x.map(f(y, _)).orElse(Some(y)))

  def map[A](g: T => A): ForEach[A] = {
    val col = new ForEach[A] {
      def foreach(f: A => Unit): Unit = self.foreach(x => f(g(x)))
    }
    if (knownSize >= 0) {
      new KnownSizeForEach(knownSize, col)
    } else {
      col
    }
  }

  def filter(p: T => Boolean): ForEach[T] = (f: T => Unit) => self.foreach(x => if (p(x)) f(x))

  def find(p: T => Boolean): Option[T] = {
    self.foreach(x => if (p(x)) return Some(x))
    None
  }

  def exists(p: T => Boolean): Boolean = find(p).isDefined

  def forall(p: T => Boolean): Boolean = !exists(x => !p(x))

  def withFilter(p: T => Boolean): WithFilter = new WithFilter(p)

  def collect[A](g: PartialFunction[T, A]): ForEach[A] = (f: A => Unit) => self.foreach(x => if (g.isDefinedAt(x)) f(g(x)))

  def to[A >: T, C](factory: Factory[A, C]): C = {
    val x = factory.newBuilder
    foreach(y => x.addOne(y))
    x.result()
  }

  def toIndexedSeq: IndexedSeq[T] = to(IndexedSeq)

  def toSeq: Seq[T] = to(Seq)

  def toSet[A >: T]: Set[A] = to(Set)

  def toArray[A >: T](implicit tag: ClassTag[A]): Array[A] = to(Array)

  def histogram[A >: T]: MapView[A, Int] = {
    val map = collection.mutable.HashMap.empty[A, IncrementalInt]
    self.foreach(map.getOrElseUpdate(_, IncrementalInt()).++)
    map.view.mapValues(_.getValue)
  }

  class WithFilter(p: T => Boolean) {
    def map[B](f: T => B): ForEach[B] = self.filter(p).map(f)

    def flatMap[B](f: T => ForEach[B]): ForEach[B] = self.filter(p).flatMap(f)

    def foreach(f: T => Unit): Unit = self.filter(p).foreach(f)

    def withFilter(q: T => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
  }
}

object ForEach {

  private class KnownSizeForEach[T](override val knownSize: Int, col: ForEach[T]) extends ForEach[T] {
    def foreach(f: T => Unit): Unit = col.foreach(f)
  }

  def empty[T]: ForEach[T] = from(Nil)

  //def apply[T](fe: (T => Unit) => Unit): ForEach[T] = (f: T => Unit) => fe(f)

  implicit def from[T](x: IterableOnce[T]): ForEach[T] = {
    val col = new ForEach[T] {
      def foreach(f: T => Unit): Unit = x.iterator.foreach(f)
    }
    if (x.knownSize >= 0) {
      new KnownSizeForEach(x.knownSize, col)
    } else {
      col
    }
  }

  def apply[T](x: T, xs: T*): ForEach[T] = {
    val col = new ForEach[T] {
      def foreach(f: T => Unit): Unit = {
        f(x)
        xs.foreach(f)
      }
    }
    new KnownSizeForEach[T](xs.length + 1, col)
  }

  implicit class PimpedDoubleForEach[T](val x: ForEach[ForEach[T]]) extends AnyVal {
    def flatten: ForEach[T] = x.flatMap(x => x)
  }

  implicit class PimpedTupleForEach[A, B](val x: ForEach[(A, B)]) extends AnyVal {
    def toMap: Map[A, B] = x.to(Map)
  }

}
