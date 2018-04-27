package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.data.ops.Transformable

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
trait Sortable[T, Coll] extends Transformable[T, Coll] {

  protected implicit val ordering: Ordering[T]

  def sorted: Coll = transform(new Traversable[T] {
    def foreach[U](f: T => U): Unit = coll.toSeq.sorted.foreach(f)
  })

  def sortBy[A](f: T => A)(implicit ord: Ordering[A]): Coll = transform(new Traversable[T] {
    def foreach[U](f2: T => U): Unit = coll.toSeq.sortBy(f).foreach(f2)
  })

}