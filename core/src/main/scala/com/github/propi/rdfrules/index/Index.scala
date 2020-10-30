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

  def tripleMap[T](f: TripleIndex[Int] => T): T

  def tripleItemMap[T](f: TripleItemIndex => T): T

  def toDataset: Dataset

  def cache(os: => OutputStream): Unit

  def cache(file: File): Index

  def cache(file: String): Index

  def withDebugger(implicit debugger: Debugger): Index

  def withEvaluatedLazyVals: Index = new IndexDecorator(this) {
    private var thiEvaluated = false

    override def tripleMap[T](f: TripleIndex[Int] => T): T = super.tripleMap { thi =>
      if (!thiEvaluated) {
        thi.evaluateAllLazyVals()
        thiEvaluated = true
      }
      f(thi)
    }

    override def withEvaluatedLazyVals: Index = this
  }

  final def mine(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer(_))): Ruleset = tripleItemMap { implicit mapper =>
    tripleMap { implicit thi =>
      ruleConsumer.invoke { ruleConsumer =>
        val result = miner.mine(ruleConsumer)
        Ruleset(this, result.rules, result.isCached)
      }
    }
  }

}

object Index {

  private abstract class FromDatasetIndex(_dataset: Option[Dataset]) extends Index with Cacheable with FromDatasetBuildable {
    @volatile protected var dataset: Option[Dataset] = _dataset
  }

  private class FromDatasetPartiallyPreservedIndex(_dataset: Option[Dataset])(implicit val debugger: Debugger) extends FromDatasetIndex(_dataset) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetPartiallyPreservedIndex(dataset)
  }

  private class FromDatasetFullyPreservedIndex(_dataset: Option[Dataset])(implicit val debugger: Debugger) extends FromDatasetIndex(_dataset) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromDatasetFullyPreservedIndex(dataset)
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

  private class FromCachePartiallyPreservedIndex(is: => InputStream)(implicit val debugger: Debugger) extends FromCacheIndex(is) with PartiallyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromCachePartiallyPreservedIndex(is)
  }

  private class FromCacheFullyPreservedIndex(is: => InputStream)(implicit val debugger: Debugger) extends FromCacheIndex(is) with FullyPreservedInMemory {
    def withDebugger(implicit debugger: Debugger): Index = new FromCacheFullyPreservedIndex(is)
  }

  def apply(dataset: Dataset, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromDatasetPartiallyPreservedIndex(Some(dataset))
    } else {
      new FromDatasetFullyPreservedIndex(Some(dataset))
    }
  }

  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): Index = {
    if (partially) {
      new FromCachePartiallyPreservedIndex(is)
    } else {
      new FromCacheFullyPreservedIndex(is)
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new File(file), partially)

}