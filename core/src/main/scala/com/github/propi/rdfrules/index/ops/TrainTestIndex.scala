package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.Index.PartType
import com.github.propi.rdfrules.index.{Index, IndexPart, MergedTripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

trait TrainTestIndex extends Index {
  def testIsTrain: Boolean

  def train: IndexPart

  def test: IndexPart

  def merged: IndexPart

  def withDebugger(implicit debugger: Debugger): TrainTestIndex
}

object TrainTestIndex {

  private class Splitted(val train: IndexPart, val test: IndexPart, _merged: Option[IndexPart]) extends Index with Cacheable with TrainTestIndex with IntervalIndexAutoLoader with DiscretizationOps {
    @volatile private var _mergedCache: Option[IndexPart] = _merged

    lazy val merged: IndexPart = _merged match {
      case Some(x) => x
      case None =>
        _mergedCache = Some(IndexPart(MergedTripleIndex(train.tripleMap, test.tripleMap), test.tripleItemMap))
        _mergedCache.get
    }

    def withMainPart(indexPart: IndexPart): Index = {
      implicit val debugger: Debugger = indexPart.debugger
      apply(indexPart, test.toDataset, true)
    }

    def testIsTrain: Boolean = false

    def main: IndexPart = train

    def part(partType: PartType): Option[IndexPart] = partType match {
      case PartType.Train => Some(train)
      case PartType.Test => Some(test)
    }

    def parts: Iterator[(PartType, IndexPart)] = Iterator(PartType.Train -> train, PartType.Test -> test)

    def tripleItemMap: TripleItemIndex = test.tripleItemMap

    def withDebugger(implicit debugger: Debugger): TrainTestIndex = new Splitted(train.withDebugger, test.withDebugger, _mergedCache.map(_.withDebugger))
  }

  def apply(train: IndexPart, test: Dataset, partially: Boolean)(implicit debugger: Debugger): TrainTestIndex = {
    val trainIndex = Index(train)
    val testIndex = IndexPart(test, trainIndex, partially)
    new Splitted(trainIndex.main, testIndex, None)
  }

  def apply(train: Dataset, test: Dataset, partially: Boolean)(implicit debugger: Debugger): TrainTestIndex = {
    val trainIndex = Index(train, partially)
    val testIndex = IndexPart(test, trainIndex, partially)
    new Splitted(trainIndex.main, testIndex, None)
  }

  def apply(index: Index): TrainTestIndex = index match {
    case x: TrainTestIndex => x
    case index =>
      index.part(Index.PartType.Test) match {
        case Some(test) => new Splitted(index.main, test, None)
        case None => new SingleIndex(index.main)
      }
  }

}