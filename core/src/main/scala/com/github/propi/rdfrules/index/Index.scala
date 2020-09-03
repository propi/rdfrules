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

  def tripleMap[T](f: TripleHashIndex[Int] => T): T

  def tripleItemMap[T](f: TripleItemHashIndex => T): T

  def toDataset: Dataset

  def cache(os: => OutputStream): Unit

  def cache(file: File): Index

  def cache(file: String): Index

  def withEvaluatedLazyVals: Index = new IndexDecorator(this) {
    private var thiEvaluated = false

    override def tripleMap[T](f: TripleHashIndex[Int] => T): T = super.tripleMap { thi =>
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
      thi.subjects
      thi.objects
      Ruleset(this, miner.mine, true)
    }
  }

}

object Index {

  def apply(_dataset: Dataset)(implicit _debugger: Debugger): Index = {
    new Index with Cacheable with FromDatasetBuildable with PartiallyPreservedInMemory {
      implicit val debugger: Debugger = _debugger

      @volatile protected var dataset: Option[Dataset] = Some(_dataset)
    }
  }

  def fromCache(is: => InputStream)(implicit _debugger: Debugger): Index = {
    new Index with Cacheable with FromCacheBuildable with PartiallyPreservedInMemory {
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
  }

  def fromCache(file: File)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file))

  def fromCache(file: String)(implicit debugger: Debugger): Index = fromCache(new File(file))

}