package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{IndexPart, TripleIndex, TripleItemIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FullyPreservedInMemory extends Buildable {

  self: IndexPart =>

  protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])]

  @volatile private var _isEvaluated = false

  private lazy val (tihi, thi) = {
    _isEvaluated = true
    defaultIndex.getOrElse(buildAll)
  }

  protected def optIndex: Option[(TripleItemIndex, TripleIndex[Int])] = defaultIndex.orElse(if (_isEvaluated) Some(tihi -> thi) else None)

  def tripleMap: TripleIndex[Int] = thi

  def tripleItemMap: TripleItemIndex = tihi

}