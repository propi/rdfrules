package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.data.ops.Transformable
import com.github.propi.rdfrules.ruleset.ops.Sortable.Sorted
import com.github.propi.rdfrules.utils.ForEach

import scala.collection.mutable

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
trait Sortable[T, Coll] extends Transformable[T, Coll] {

  protected implicit val ordering: Ordering[T]

  def sorted: Coll = transform(new Sorted(coll))

  def sortBy[A](f: T => A)(implicit ord: Ordering[A]): Coll = transform(new Sorted(coll)(Ordering.by[T, A](f)))

  def reversed: Coll = transform((f: T => Unit) => coll.toIndexedSeq.reverseIterator.foreach(f))

}

object Sortable {

  private class Sorted[T](col: ForEach[T])(implicit ord: Ordering[T]) extends ForEach[T] {
    private lazy val sorted = {
      val queue = mutable.PriorityQueue.empty[T]
      col.foreach(queue.enqueue(_))
      queue.dequeueAll.asInstanceOf[IndexedSeq[T]]
    }

    def foreach(f: T => Unit): Unit = sorted.foreach(f)

    override def knownSize: Int = sorted.knownSize
  }

}