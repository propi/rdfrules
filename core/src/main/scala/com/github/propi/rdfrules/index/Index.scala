package com.github.propi.rdfrules.index

import java.io._
import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.IndexCollections.Builder
import com.github.propi.rdfrules.index.ops._
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Index {
  implicit val debugger: Debugger

  def tripleMap: TripleIndex[Int]

  def tripleItemMap: TripleItemIndex

  def toDataset: Dataset

  def cache(os: => OutputStream): Unit

  def cache(file: File): Index

  final def cache(file: String): Index = cache(new File(file))

  final def properties: Iterator[PropertyCardinalities.Mapped] = tripleMap.predicates.iterator.map(x => PropertyCardinalities(x, tripleMap.predicates(x)))

  final def properties(filter: Set[Int]): Iterator[PropertyCardinalities.Mapped] = filter.iterator.flatMap(x => tripleMap.predicates.get(x).map(PropertyCardinalities(x, _)))

  def withDebugger(implicit debugger: Debugger): Index

  def withEvaluatedLazyVals: Index = new IndexDecorator(this) {
    @volatile private var thiEvaluated = false

    override def tripleMap: TripleIndex[Int] = {
      val thi = super.tripleMap
      if (!thiEvaluated) {
        thi.evaluateAllLazyVals()
        thiEvaluated = true
      }
      thi
    }

    override def withEvaluatedLazyVals: Index = this
  }

  final def mineRules(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer())): Ruleset = {
    implicit val thi: TripleIndex[Int] = tripleMap
    implicit val mapper: TripleItemIndex = tripleItemMap
    ruleConsumer.invoke { ruleConsumer =>
      val result = miner.mine(ruleConsumer)
      Ruleset(IndexContainer.Single(this), result)
    }
  }
}

object Index {

  /*  sealed trait PartType

    object PartType {
      case object Training extends PartType

      case object Test extends PartType

      case object TrainingTest extends PartType
    }*/

  private abstract class FromDatasetIndex(_dataset: Option[Dataset], _parent: Option[Index]) extends Index with Cacheable with FromDatasetBuildable {
    @volatile protected var dataset: Option[Dataset] = _dataset
    @volatile protected var parent: Option[Index] = _parent
  }

  private class FromDatasetPartiallyPreservedIndex(_dataset: Option[Dataset],
                                                   _parent: Option[Index],
                                                   protected val defaultTripleMap: Option[TripleIndex[Int]],
                                                   protected val defaultTripleItemMap: Option[TripleItemIndex])
                                                  (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset, _parent) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetPartiallyPreservedIndex(dataset, parent, optTripleMap, optTripleItemMap)
  }

  private class FromDatasetFullyPreservedIndex(_dataset: Option[Dataset],
                                               _parent: Option[Index],
                                               protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])])
                                              (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset, _parent) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetFullyPreservedIndex(dataset, parent, optIndex)
  }

  private abstract class FromCacheIndex(is: => InputStream) extends Index with Cacheable with FromCacheBuildable {
    protected def useInputStream[T](f: InputStream => T): T = {
      val _is = is
      try {
        f(_is)
      } finally {
        _is.close()
      }
    }

    override def cache(os: => OutputStream): Unit = super[FromCacheBuildable].cache(os)
  }

  private class FromCachePartiallyPreservedIndex(is: => InputStream,
                                                 protected val defaultTripleMap: Option[TripleIndex[Int]],
                                                 protected val defaultTripleItemMap: Option[TripleItemIndex])
                                                (implicit val debugger: Debugger) extends FromCacheIndex(is) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromCachePartiallyPreservedIndex(is, optTripleMap, optTripleItemMap)
  }

  private class FromCacheFullyPreservedIndex(is: => InputStream,
                                             protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])])
                                            (implicit val debugger: Debugger) extends FromCacheIndex(is) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromCacheFullyPreservedIndex(is, optIndex)
  }

  private class FromIndicesIndex(tripleMap: TripleIndex[Int], tripleItemMap: TripleItemIndex)(implicit val debugger: Debugger) extends Index with FullyPreservedInMemory with Cacheable {
    def withDebugger(implicit debugger: Debugger): Index = new FromIndicesIndex(tripleMap, tripleItemMap)

    protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])] = None

    protected def buildTripleIndex: TripleIndex[Int] = tripleMap

    protected def buildTripleItemIndex: TripleItemIndex = tripleItemMap

    protected def buildAll: (TripleItemIndex, TripleIndex[Int]) = tripleItemMap -> tripleMap
  }

  def apply(dataset: Dataset, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset), None, None, None)
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset), None, None)
    }
  }

  def apply(dataset: Dataset, parent: Index, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset), Some(parent), None, None)
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset), Some(parent), None)
    }
  }

  def apply(tripleMap: TripleIndex[Int], tripleItemMap: TripleItemIndex)(implicit debugger: Debugger): Index = new FromIndicesIndex(tripleMap, tripleItemMap)

  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromCachePartiallyPreservedIndex(is, None, None)
    } else {
      new FromCacheFullyPreservedIndex(is, None)
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new File(file), partially)

  implicit def indexToBuilder(index: Index): Builder[Int] = new Builder[Int] {
    def build: TripleIndex[Int] = index.tripleMap
  }

}