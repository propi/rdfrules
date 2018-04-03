package com.github.propi.rdfrules.index.ops

import java.io.OutputStream

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.ops.Cacheable.SerItem
import com.github.propi.rdfrules.index.{CompressedQuad, Index}
import com.github.propi.rdfrules.utils.serialization.Serializer
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.serialization.CompressedQuadSerialization._

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Cacheable extends QuadsIndex {

  self: Index =>

  def cache(os: => OutputStream): Unit = Serializer.serializeToOutputStream[SerItem](os) { writer =>
    self.tripleItemMap(_.iterator.foreach(x => writer.write(Left(x))))
    compressedQuads.foreach(x => writer.write(Right(x)))
  }

}

object Cacheable {

  type SerItem = Either[(Int, TripleItem), CompressedQuad]

}