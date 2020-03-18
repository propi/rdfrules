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

  def newIndex: Index

  def withEvaluatedLazyVals: Index

  def mine(miner: RulesMining): Ruleset = tripleItemMap { implicit mapper =>
    tripleMap { implicit thi =>
      Ruleset(this, miner.mine)
    }
  }

}

object Index {

  sealed trait Mode

  object Mode {

    case object InUseInMemory extends Mode

    case object PreservedInMemory extends Mode

  }

  def apply(dataset: Dataset, mode: Mode = Mode.PreservedInMemory)(implicit debugger: Debugger): Index = {
    val _dataset = dataset
    val _debugger = debugger
    trait ConcreteIndex extends Index with Cacheable with FromDatasetBuildable {
      implicit val debugger: Debugger = _debugger

      override def toDataset: Dataset = _dataset

      def newIndex: Index = apply(_dataset, mode)
    }
    mode match {
      case Mode.PreservedInMemory => new ConcreteIndex with PreservedInMemory
      case Mode.InUseInMemory => new ConcreteIndex with InUseInMemory
    }
  }

  def fromCache(is: => InputStream, mode: Mode)(implicit debugger: Debugger): Index = {
    val _debugger = debugger
    trait ConcreteIndex extends Index with Cacheable with FromCacheBuildable {
      implicit val debugger: Debugger = _debugger

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

  def fromCache(is: => InputStream)(implicit debugger: Debugger): Index = fromCache(is, Mode.PreservedInMemory)(debugger)

  def fromCache(file: File, mode: Mode)(implicit debugger: Debugger): Index = fromCache(new FileInputStream(file), mode)

  def fromCache(file: String, mode: Mode)(implicit debugger: Debugger): Index = fromCache(new File(file), mode)

  def fromCache(file: File)(implicit debugger: Debugger): Index = fromCache(file, Mode.PreservedInMemory)

  def fromCache(file: String)(implicit debugger: Debugger): Index = fromCache(new File(file))

}