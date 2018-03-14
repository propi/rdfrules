package eu.easyminer.rdf.index

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.data.Dataset
import eu.easyminer.rdf.index.ops._

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Index {

  def tripleMap[T](f: TripleHashIndex => T): T

  def tripleItemMap[T](f: TripleItemHashIndex => T): T

  def toDataset: Dataset

  def cache(os: => OutputStream): Unit

  def newIndex: Index

}

object Index {

  sealed trait Mode

  object Mode {

    case object InUseInMemory extends Mode

    case object PreservedInMemory extends Mode

  }

  def fromDataset(dataset: Dataset, mode: Mode = Mode.PreservedInMemory): Index = {
    val _dataset = dataset
    trait ConcreteIndex extends Index with Cacheable with FromDatasetBuildable {
      override def toDataset: Dataset = _dataset

      def newIndex: Index = fromDataset(_dataset, mode)
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