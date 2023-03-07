package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.ops._
import com.github.propi.rdfrules.utils.Debugger

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait IndexPart {
  implicit val debugger: Debugger

  def tripleMap: TripleIndex[Int]

  def tripleItemMap: TripleItemIndex

  def toDataset: Dataset

  //  def cache(os: => OutputStream): Unit
  //
  //  def cache(file: File): IndexPart
  //
  //  final def cache(file: String): IndexPart = cache(new File(file))

  final def properties: Iterator[PropertyCardinalities.Mapped] = tripleMap.predicates.iterator.map(x => PropertyCardinalities(x, tripleMap.predicates(x)))

  final def properties(filter: Set[Int]): Iterator[PropertyCardinalities.Mapped] = filter.iterator.flatMap(x => tripleMap.predicates.get(x).map(PropertyCardinalities(x, _)))

  def withDebugger(implicit debugger: Debugger): IndexPart

  //  def withEvaluatedLazyVals: IndexPart = new IndexDecorator(this) {
  //    @volatile private var thiEvaluated = false
  //
  //    override def tripleMap: TripleIndex[Int] = {
  //      val thi = super.tripleMap
  //      if (!thiEvaluated) {
  //        thi.evaluateAllLazyVals()
  //        thiEvaluated = true
  //      }
  //      thi
  //    }
  //
  //    override def withEvaluatedLazyVals: IndexPart = this
  //  }
}

object IndexPart {

  /*  sealed trait PartType

    object PartType {
      case object Training extends PartType

      case object Test extends PartType

      case object TrainingTest extends PartType
    }*/

  private abstract class FromDatasetIndex(_dataset: Option[Dataset], _parentTripleItemIndex: Option[TripleItemIndex], _parentTripleIndexes: Seq[TripleIndex[Int]]) extends IndexPart with FromDatasetBuildable with QuadsIndex {
    @volatile protected var dataset: Option[Dataset] = _dataset
    @volatile protected var parentTripleItemIndex: Option[TripleItemIndex] = _parentTripleItemIndex
    @volatile protected var parentTripleIndexes: Seq[TripleIndex[Int]] = _parentTripleIndexes
  }

  private class FromDatasetPartiallyPreservedIndex(_dataset: Option[Dataset],
                                                   _parentTripleItemIndex: Option[TripleItemIndex],
                                                   _parentTripleIndexes: Seq[TripleIndex[Int]],
                                                   protected val defaultTripleMap: Option[TripleIndex[Int]],
                                                   protected val defaultTripleItemMap: Option[TripleItemIndex])
                                                  (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset, _parentTripleItemIndex, _parentTripleIndexes) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): IndexPart = new FromDatasetPartiallyPreservedIndex(dataset, parentTripleItemIndex, parentTripleIndexes, optTripleMap, optTripleItemMap)
  }

  private class FromDatasetFullyPreservedIndex(_dataset: Option[Dataset],
                                               _parentTripleItemIndex: Option[TripleItemIndex],
                                               _parentTripleIndexes: Seq[TripleIndex[Int]],
                                               protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])])
                                              (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset, _parentTripleItemIndex, _parentTripleIndexes) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): IndexPart = new FromDatasetFullyPreservedIndex(dataset, parentTripleItemIndex, parentTripleIndexes, optIndex)
  }

  //  private abstract class FromCacheIndex(is: => InputStream) extends IndexPart with Cacheable with FromCacheBuildable {
  //    protected def useInputStream[T](f: InputStream => T): T = {
  //      val _is = is
  //      try {
  //        f(_is)
  //      } finally {
  //        _is.close()
  //      }
  //    }
  //
  //    override def cache(os: => OutputStream): Unit = super[FromCacheBuildable].cache(os)
  //  }
  //
  //  private class FromCachePartiallyPreservedIndex(is: => InputStream,
  //                                                 protected val defaultTripleMap: Option[TripleIndex[Int]],
  //                                                 protected val defaultTripleItemMap: Option[TripleItemIndex])
  //                                                (implicit val debugger: Debugger) extends FromCacheIndex(is) with PartiallyPreservedInMemory {
  //    def withDebugger(implicit debugger: Debugger): IndexPart = new FromCachePartiallyPreservedIndex(is, optTripleMap, optTripleItemMap)
  //  }
  //
  //  private class FromCacheFullyPreservedIndex(is: => InputStream,
  //                                             protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])])
  //                                            (implicit val debugger: Debugger) extends FromCacheIndex(is) with FullyPreservedInMemory {
  //    def withDebugger(implicit debugger: Debugger): IndexPart = new FromCacheFullyPreservedIndex(is, optIndex)
  //  }
  //
  private class FromIndicesIndex(tripleMap: TripleIndex[Int], tripleItemMap: TripleItemIndex)(implicit val debugger: Debugger) extends IndexPart with FullyPreservedInMemory with QuadsIndex {
    def withDebugger(implicit debugger: Debugger): IndexPart = new FromIndicesIndex(tripleMap, tripleItemMap)

    protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])] = None

    protected def buildTripleIndex: TripleIndex[Int] = tripleMap

    protected def buildTripleItemIndex: TripleItemIndex = tripleItemMap

    protected def buildAll: (TripleItemIndex, TripleIndex[Int]) = tripleItemMap -> tripleMap
  }

  def apply(dataset: Dataset, partially: Boolean)(implicit _debugger: Debugger): IndexPart = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset), None, Nil, None, None)
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset), None, Nil, None)
    }
  }

  def apply(tripleMap: TripleIndex[Int], tripleItemMap: TripleItemIndex)(implicit debugger: Debugger): IndexPart = new FromIndicesIndex(tripleMap, tripleItemMap)

  def apply(dataset: Dataset, parent: Index, partially: Boolean)(implicit _debugger: Debugger): IndexPart = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset), Some(parent.tripleItemMap), parent.parts.map(_._2.tripleMap).toList, None, None)
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset), Some(parent.tripleItemMap), parent.parts.map(_._2.tripleMap).toList, None)
    }
  }

  //  def apply(tripleMap: TripleIndex[Int], tripleItemMap: TripleItemIndex)(implicit debugger: Debugger): IndexPart = new FromIndicesIndex(tripleMap, tripleItemMap)
  //
  //  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): IndexPart = {
  //    if (partially) {
  //      new FromCachePartiallyPreservedIndex(is, None, None)
  //    } else {
  //      new FromCacheFullyPreservedIndex(is, None)
  //    }
  //  }
  //
  //  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): IndexPart = fromCache(new FileInputStream(file), partially)
  //
  //  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): IndexPart = fromCache(new File(file), partially)
  //
  //  implicit def indexToBuilder(index: IndexPart): Builder[Int] = new Builder[Int] {
  //    def build: TripleIndex[Int] = index.tripleMap
  //  }

}