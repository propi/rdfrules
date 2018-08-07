package com.github.propi.rdfrules.index.ops

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}

import com.github.propi.rdfrules.index.ops.Cacheable.SerItem
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.utils.serialization.Deserializer
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.serialization.CompressedQuadSerialization._


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
    val _os = new BufferedOutputStream(os)
    try {
      useInputStream { is =>
        val _is = new BufferedInputStream(is)
        try {
          Stream.continually(is.read()).takeWhile(_ != -1).foreach(_os.write)
        } finally {
          _is.close()
        }
      }
    } finally {
      _os.close()
    }
  }

}