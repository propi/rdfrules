package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.ops.CollectionBuilders.intCollectionBuilder
import com.github.propi.rdfrules.index.{Index, IndexPart, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

import java.io.InputStream

class CachedWholeIndex private[index](_allIndexes: Option[(TripleItemIndex, Seq[(Index.PartType, IndexPart)])], is: () => InputStream)(implicit debugger: Debugger) extends Index with Cacheable with IntervalIndexAutoLoader with DiscretizationOps {
  @volatile private var _allIndexesCache: Option[(TripleItemIndex, Seq[(Index.PartType, IndexPart)])] = _allIndexes

  private lazy val allIndexes: (TripleItemIndex, Seq[(Index.PartType, IndexPart)]) = _allIndexesCache match {
    case Some(x) => x
    case None =>
      val (tim, data) = Cacheable.loadAll(is)
      _allIndexesCache = Some(tim -> data.map(x => x._1 -> IndexPart(x._2, tim)))
      _allIndexesCache.get
  }

  def withMainPart(indexPart: IndexPart): Index = {
    val _tripleIndexes = allIndexes._2.tail.foldLeft(Vector(allIndexes._2.head._1 -> indexPart)) { case (col, (partType, indexPart)) =>
      col :+ (partType -> IndexPart(indexPart.toDataset, col.last._2, false))
    }
    new CachedWholeIndex(
      Some(_tripleIndexes.last._2.tripleItemMap -> _tripleIndexes),
      is
    )
  }

  def tripleItemMap: TripleItemIndex = allIndexes._1

  def main: IndexPart = allIndexes._2.head._2

  def part(partType: Index.PartType): Option[IndexPart] = allIndexes._2.find(_._1 == partType).map(_._2)

  def parts: Iterator[(Index.PartType, IndexPart)] = allIndexes._2.iterator

  def withDebugger(implicit debugger: Debugger): Index = new CachedWholeIndex(_allIndexesCache.map(x => x._1 -> x._2.map(x => x._1 -> x._2.withDebugger)), is)
}
