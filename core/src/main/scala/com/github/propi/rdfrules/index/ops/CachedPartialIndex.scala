package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.ops.CollectionBuilders.intCollectionBuilder
import com.github.propi.rdfrules.index.{Index, IndexPart, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

import java.io.InputStream

class CachedPartialIndex private[index](_tripleItemIndex: Option[TripleItemIndex], _tripleIndexes: Option[Seq[(Index.PartType, IndexPart)]], is: () => InputStream)(implicit debugger: Debugger) extends Index with Cacheable with IntervalIndexAutoLoader with DiscretizationOps {

  @volatile private var _tripleItemIndexCache: Option[TripleItemIndex] = _tripleItemIndex
  @volatile private var _tripleIndexesCache: Option[Seq[(Index.PartType, IndexPart)]] = _tripleIndexes

  def withMainPart(indexPart: IndexPart): Index = {
    val _tripleIndexes = tripleIndexes.tail.foldLeft(Vector(tripleIndexes.head._1 -> indexPart)) { case (col, (partType, indexPart)) =>
      col :+ (partType -> IndexPart(indexPart.toDataset, col.last._2, true))
    }
    new CachedPartialIndex(
      Some(_tripleIndexes.last._2.tripleItemMap),
      Some(_tripleIndexes),
      is
    )
  }

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
