package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleIndex, TripleItemIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait PartiallyPreservedInMemory extends Buildable {

  self: Index =>

  private lazy val thi = buildTripleIndex
  private lazy val tihi = buildTripleItemIndex

  def tripleMap[T](f: TripleIndex[Int] => T): T = f(thi)

  def tripleItemMap[T](f: TripleItemIndex => T): T = f(tihi)

}
