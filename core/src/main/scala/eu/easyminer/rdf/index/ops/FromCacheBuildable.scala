package eu.easyminer.rdf.index.ops

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.index.ops.Cacheable.SerItem
import eu.easyminer.rdf.index.{Index, TripleHashIndex, TripleItemHashIndex}
import eu.easyminer.rdf.utils.serialization.Deserializer
import eu.easyminer.rdf.serialization.TripleItemSerialization._
import eu.easyminer.rdf.serialization.CompressedQuadSerialization._


/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromCacheBuildable extends Buildable {

  self: Index =>

  protected def useInputStream[T](f: InputStream => T): T

  private def cachedItems: Traversable[SerItem] = new Traversable[SerItem] {
    def foreach[U](f: SerItem => U): Unit = useInputStream { is =>
      Deserializer.deserializeFromInputStream(is) { reader: Deserializer.Reader[SerItem] =>
        Stream.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
      }
    }
  }

  protected def buildTripleHashIndex: TripleHashIndex = TripleHashIndex(cachedItems.view.collect {
    case Right(x) => x
  })

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex.fromIndexedItem(cachedItems.view.collect {
    case Left(x) => x
  })

  def cache(os: => OutputStream): Unit = {
    val _os = os
    try {
      useInputStream { is =>
        Stream.continually(is.read()).takeWhile(_ != -1).foreach(_os.write)
      }
    } finally {
      _os.close()
    }
  }

}