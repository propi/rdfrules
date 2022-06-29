package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.ops.Cacheable.SerItem
import com.github.propi.rdfrules.index.{Index, IndexItem}
import com.github.propi.rdfrules.serialization.IndexItemSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.Serializer

import java.io.{File, FileOutputStream, OutputStream}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Cacheable extends QuadsIndex {

  self: Index =>

  def cache(os: => OutputStream): Unit = Serializer.serializeToOutputStream[SerItem](os) { writer =>
    debugger.debug("Triple items caching") { ad =>
      self.tripleItemMap.iterator.foreach(x => writer.write(Left(x)))
      ad.done()
    }
    debugger.debug("Triples caching") { ad =>
      compressedQuads.foreach { x =>
        writer.write(Right(x))
        ad.done()
      }
    }
  }

  def cache(file: File): Index = {
    cache(new FileOutputStream(file))
    this
  }

  def cache(file: String): Index = cache(new File(file))

}

object Cacheable {

  type SerItem = Either[(Int, TripleItem), IndexItem.IntQuad]

}