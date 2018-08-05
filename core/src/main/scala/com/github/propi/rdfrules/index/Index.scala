package com.github.propi.rdfrules.index

import java.io.{File, FileInputStream, InputStream, OutputStream}

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

  def cache(file: File): Unit

  def cache(file: String): Unit

  def newIndex: Index

  def withEvaluatedLazyVals: Index

  def mine(miner: RulesMining): Ruleset = tripleItemMap { implicit mapper =>
    tripleMap { implicit thi =>
      Ruleset(miner.mine, this)
    }
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

  def fromCache(is: => InputStream, mode: Mode): Index = {
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

  def fromCache(is: => InputStream): Index = fromCache(is, Mode.PreservedInMemory)

  def fromCache(file: File, mode: Mode): Index = fromCache(new FileInputStream(file), mode)

  def fromCache(file: String, mode: Mode): Index = fromCache(new File(file), mode)

  def fromCache(file: File): Index = fromCache(file, Mode.PreservedInMemory)

  def fromCache(file: String): Index = fromCache(new File(file))

}