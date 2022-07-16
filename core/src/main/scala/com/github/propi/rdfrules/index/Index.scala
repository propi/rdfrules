package com.github.propi.rdfrules.index

import java.io._

import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.ops._
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

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

  def cache(file: String): Index = cache(new File(file))

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

  final def mine(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer(_))): Ruleset = {
    implicit val thi: TripleIndex[Int] = tripleMap
    implicit val mapper: TripleItemIndex = tripleItemMap
    ruleConsumer.invoke { ruleConsumer =>
      val result = miner.mine(ruleConsumer)
      Ruleset(this, result)
    }
  }

}

object Index {

  private abstract class FromDatasetIndex(_dataset: Option[Dataset]) extends Index with Cacheable with FromDatasetBuildable {
    @volatile protected var dataset: Option[Dataset] = _dataset
  }

  private class FromDatasetPartiallyPreservedIndex(_dataset: Option[Dataset],
                                                   protected val defaultTripleMap: Option[TripleIndex[Int]],
                                                   protected val defaultTripleItemMap: Option[TripleItemIndex])
                                                  (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetPartiallyPreservedIndex(dataset, optTripleMap, optTripleItemMap)
  }

  private class FromDatasetFullyPreservedIndex(_dataset: Option[Dataset],
                                               protected val defaultIndex: Option[(TripleItemIndex, TripleIndex[Int])])
                                              (implicit val debugger: Debugger) extends FromDatasetIndex(_dataset) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetFullyPreservedIndex(dataset, optIndex)
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

  def apply(dataset: Dataset, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset), None, None)
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset), None)
    }
  }

  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromCachePartiallyPreservedIndex(is, None, None)
    } else {
      new FromCacheFullyPreservedIndex(is, None)
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new File(file), partially)

}