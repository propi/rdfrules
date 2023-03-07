package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.ops.CollectionBuilders.intCollectionBuilder
import com.github.propi.rdfrules.index.{Index, IndexPart, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

import java.io.InputStream

class CachedPartialIndex private[index](_tripleItemIndex: Option[TripleItemIndex], _tripleIndexes: Option[Seq[(Index.PartType, IndexPart)]], is: () => InputStream)(implicit debugger: Debugger) extends Index with Cacheable {
  @volatile private var _tripleItemIndexCache: Option[TripleItemIndex] = _tripleItemIndex
  @volatile private var _tripleIndexesCache: Option[Seq[(Index.PartType, IndexPart)]] = _tripleIndexes

  lazy val tripleItemMap: TripleItemIndex = _tripleItemIndex match {
    case Some(x) => x
    case None =>
      _tripleItemIndexCache = Some(Cacheable.loadTripleItemIndex(is))
      _tripleItemIndexCache.get
  }

  private lazy val tripleIndexes = _tripleIndexes match {
    case Some(x) => x
    case None =>
      val tim = tripleItemMap
      _tripleIndexesCache = Some(Cacheable.loadTripleIndexes(is).map(x => x._1 -> IndexPart(x._2, tim)))
      _tripleIndexesCache.get
  }

  def main: IndexPart = tripleIndexes.head._2

  def part(partType: Index.PartType): Option[IndexPart] = tripleIndexes.find(_._1 == partType).map(_._2)

  def parts: Iterator[(Index.PartType, IndexPart)] = tripleIndexes.iterator

  def withDebugger(implicit debugger: Debugger): Index = new CachedPartialIndex(_tripleItemIndexCache, _tripleIndexesCache.map(_.map(x => x._1 -> x._2.withDebugger)), is)
}
