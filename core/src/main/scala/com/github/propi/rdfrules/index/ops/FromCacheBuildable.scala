package com.github.propi.rdfrules.index.ops

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.ops.Cacheable.SerItem
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.serialization.CompressedQuadSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.Deserializer


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

  protected def buildTripleHashIndex: TripleHashIndex[Int] = TripleHashIndex(cachedItems.view.collect {
    case Right(x) => new TripleHashIndex.Quad(x.subject, x.predicate, x.`object`, x.graph)
  })

  protected def buildTripleItemHashIndex: TripleItemHashIndex = TripleItemHashIndex.fromIndexedItem(new Traversable[(Int, TripleItem)] {
    def foreach[U](f: ((Int, TripleItem)) => U): Unit = {
      val prefixes = collection.mutable.Map.empty[String, Prefix]
      for ((num, tripleItem) <- cachedItems.view.collect { case Left(x) => x }) {
        f(num -> (tripleItem match {
          case TripleItem.PrefixedUri(prefix, localName) =>
            val loadedPrefix = prefixes.getOrElseUpdate(prefix.nameSpace, prefix)
            if (loadedPrefix eq prefix) tripleItem else TripleItem.PrefixedUri(loadedPrefix, localName)
          case x => x
        }))
      }
    }
  })

  def cache(os: => OutputStream): Unit = {
    val _os = new BufferedOutputStream(os)
    try {
      useInputStream { is =>
        val _is = new BufferedInputStream(is)
        try {
          debugger.debug("Index caching") { ad =>
            Stream.continually(is.read()).takeWhile(_ != -1).foreach { x =>
              _os.write(x)
              ad.done()
            }
          }
        } finally {
          _is.close()
        }
      }
    } finally {
      _os.close()
    }
  }

}