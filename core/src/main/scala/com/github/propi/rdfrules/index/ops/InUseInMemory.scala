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

  def withEvaluatedLazyVals: Index = new IndexDecorator(this) {
    private lazy val thiEvaluated = new DynamicVariable[Boolean](false)

    override def tripleMap[T](f: TripleHashIndex => T): T = super.tripleMap { thi =>
      if (!thiEvaluated.value) {
        thi.evaluateAllLazyVals()
        thiEvaluated.withValue(true)(f(thi))
      } else {
        f(thi)
      }
    }

    override def withEvaluatedLazyVals: Index = this
  }

}
