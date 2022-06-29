package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.IndexItem.IntQuad
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.index.ops.Cacheable.SerItem
import com.github.propi.rdfrules.serialization.IndexItemSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.Deserializer

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}


/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait FromCacheBuildable extends Buildable {

  self: Index =>

  protected def useInputStream[T](f: InputStream => T): T

  private def cachedItems[T](f: (Iterator[(Int, TripleItem)], Iterator[IndexItem.IntQuad]) => T): T = {
    useInputStream { is =>
      Deserializer.deserializeFromInputStream(is) { reader: Deserializer.Reader[SerItem] =>
        val (it1, it2) = Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).span(_.isLeft)
        f(it1.map(_.left.toOption.get), it2.map(_.toOption.get))
      }
    }
  }

  private def _buildTripleIndex(it: Iterator[IndexItem.IntQuad]): TripleHashIndex[Int] = TripleHashIndex((f: IntQuad => Unit) => it.foreach(f))

  protected def buildTripleIndex: TripleIndex[Int] = cachedItems { (it1, it2) =>
    it1.foreach(_ => ())
    _buildTripleIndex(it2)
  }

  private def _buildTripleItemIndex(it: Iterator[(Int, TripleItem)]): TripleItemIndex = TripleItemHashIndex.fromIndexedItem((f: ((Int, TripleItem)) => Unit) => {
    val prefixes = collection.mutable.Map.empty[String, Prefix]
    for ((num, tripleItem) <- it) {
      f(num -> (tripleItem match {
        case TripleItem.PrefixedUri(prefix, localName) =>
          val loadedPrefix = prefixes.getOrElseUpdate(prefix.nameSpace, prefix)
          if (loadedPrefix eq prefix) tripleItem else TripleItem.PrefixedUri(loadedPrefix, localName)
        case x => x
      }))
    }
  })

  protected def buildTripleItemIndex: TripleItemIndex = cachedItems { (it1, _) =>
    _buildTripleItemIndex(it1)
  }

  protected def buildAll: (TripleItemIndex, TripleIndex[Int]) = cachedItems { (it1, it2) =>
    val tihi = _buildTripleItemIndex(it1)
    val thi = _buildTripleIndex(it2)
    tihi -> thi
  }

  def cache(os: => OutputStream): Unit = {
    val _os = new BufferedOutputStream(os)
    try {
      useInputStream { is =>
        val _is = new BufferedInputStream(is)
        try {
          debugger.debug("Index caching") { ad =>
            Iterator.continually(is.read()).takeWhile(_ != -1).foreach { x =>
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