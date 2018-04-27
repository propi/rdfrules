package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

import scala.util.DynamicVariable

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait InUseInMemory extends Buildable {

  self: Index =>

  private lazy val thi = new DynamicVariable[Option[TripleHashIndex]](None)
  private lazy val tihi = new DynamicVariable[Option[TripleItemHashIndex]](None)

  def tripleMap[T](f: TripleHashIndex => T): T = thi.value match {
    case Some(x) => f(x)
    case None =>
      val x = buildTripleHashIndex
      thi.withValue(Some(x))(f(x))
  }

  def tripleItemMap[T](f: TripleItemHashIndex => T): T = tihi.value match {
    case Some(x) => f(x)
    case None =>
      val x = buildTripleItemHashIndex
      tihi.withValue(Some(x))(f(x))
  }

}
