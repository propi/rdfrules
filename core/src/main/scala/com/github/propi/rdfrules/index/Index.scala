package com.github.propi.rdfrules.index

import java.io._

import com.github.propi.rdfrules.algorithm.RulesMining
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

  final def mine(miner: RulesMining): Ruleset = tripleItemMap { implicit mapper =>
    tripleMap { implicit thi =>
      Ruleset(this, miner.mine, true)
    }
  }

}

object Index {

  def apply(_dataset: Dataset, partially: Boolean)(implicit _debugger: Debugger): Index = {
    trait FromDataset extends Index with Cacheable with FromDatasetBuildable {
      implicit val debugger: Debugger = _debugger

      @volatile protected var dataset: Option[Dataset] = Some(_dataset)
    }
    if (partially) {
      new FromDataset with PartiallyPreservedInMemory
    } else {
      new FromDataset with FullyPreservedInMemory
    }
  }

  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): Index = {
    trait FromCache extends Index with Cacheable with FromCacheBuildable {
      implicit val debugger: Debugger = _debugger

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
    if (partially) {
      new FromCache with PartiallyPreservedInMemory
    } else {
      new FromCache with FullyPreservedInMemory
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): Index = fromCache(new File(file), partially)

}