package com.github.propi.rdfrules.ruleset.ops

import com.github.propi.rdfrules.data.ops.Transformable

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
trait Sortable[T, Coll] extends Transformable[T, Coll] {

  protected def seqColl: Seq[T]

  protected implicit val ordering: Ordering[T]

  def sorted: Coll = transform(seqColl.view.sorted)

  def sortBy[A](f: T => A)(implicit ord: Ordering[A]): Coll = transform(seqColl.sortBy(f))

}
