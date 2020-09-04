package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{Index, TripleIndex, TripleItemIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FullyPreservedInMemory extends Buildable {

  self: Index =>

  private lazy val (tihi, thi) = buildAll

  def tripleMap[T](f: TripleIndex[Int] => T): T = f(thi)

  def tripleItemMap[T](f: TripleItemIndex => T): T = f(tihi)

}
