package com.github.propi.rdfrules.index

import java.io.{InputStream, OutputStream}

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.ops._
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Index {

  def tripleMap[T](f: TripleHashIndex => T): T

  def tripleItemMap[T](f: TripleItemHashIndex => T): T

  def toDataset: Dataset

  def cache(os: => OutputStream): Unit

  def newIndex: Index

  def mine(miner: RulesMining): Ruleset = tripleMap { implicit thi =>
    Ruleset(miner.mine, this)
  }

}

object Index {

  sealed trait Mode

  object Mode {

    case object InUseInMemory extends Mode

    case object PreservedInMemory extends Mode

  }

  def apply(dataset: Dataset, mode: Mode = Mode.PreservedInMemory): Index = {
    val _dataset = dataset
    trait ConcreteIndex extends Index with Cacheable with FromDatasetBuildable {
      override def toDataset: Dataset = _dataset

      def newIndex: Index = apply(_dataset, mode)
    }
    mode match {
      case Mode.PreservedInMemory => new ConcreteIndex with PreservedInMemory
      case Mode.InUseInMemory => new ConcreteIndex with InUseInMemory
    }
  }

  def fromCache(is: => InputStream, mode: Mode = Mode.PreservedInMemory): Index = {
    trait ConcreteIndex extends Index with Cacheable with FromCacheBuildable {
      def newIndex: Index = fromCache(is, mode)

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
    mode match {
      case Mode.PreservedInMemory => new ConcreteIndex with PreservedInMemory
      case Mode.InUseInMemory => new ConcreteIndex with InUseInMemory
    }
  }

}