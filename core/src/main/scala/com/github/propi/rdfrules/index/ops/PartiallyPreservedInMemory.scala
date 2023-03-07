package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.{IndexPart, TripleIndex, TripleItemIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait PartiallyPreservedInMemory extends Buildable {

  self: IndexPart =>

  protected val defaultTripleMap: Option[TripleIndex[Int]]
  protected val defaultTripleItemMap: Option[TripleItemIndex]

  @volatile private var _isThiEvaluated = false
  @volatile private var _isTihiEvaluated = false

  private lazy val thi: TripleIndex[Int] = {
    _isThiEvaluated = true
    defaultTripleMap.getOrElse(buildTripleIndex)
  }

  private lazy val tihi: TripleItemIndex = {
    _isTihiEvaluated = true
    defaultTripleItemMap.getOrElse(buildTripleItemIndex)
  }

  protected def optTripleMap: Option[TripleIndex[Int]] = defaultTripleMap.orElse(if (_isThiEvaluated) Some(thi) else None)

  protected def optTripleItemMap: Option[TripleItemIndex] = defaultTripleItemMap.orElse(if (_isTihiEvaluated) Some(tihi) else None)

  def tripleMap: TripleIndex[Int] = thi

  def tripleItemMap: TripleItemIndex = tihi

}
