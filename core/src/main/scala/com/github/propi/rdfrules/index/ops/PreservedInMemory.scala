package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait PreservedInMemory extends Buildable {

  self: Index =>

  private lazy val thi = buildTripleHashIndex
  private lazy val tihi = buildTripleItemHashIndex

  def tripleMap[T](f: TripleHashIndex => T): T = f(thi)

  def tripleItemMap[T](f: TripleItemHashIndex => T): T = f(tihi)

  def withEvaluatedLazyVals: Index = new IndexDecorator(this) {
    private var thiEvaluated = false

    override def tripleMap[T](f: TripleHashIndex => T): T = super.tripleMap { thi =>
      if (!thiEvaluated) {
        thi.evaluateAllLazyVals()
        thiEvaluated = true
      }
      f(thi)
    }

    override def withEvaluatedLazyVals: Index = this
  }

}
