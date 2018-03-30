package eu.easyminer.rdf.index.ops

import java.io.OutputStream

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.index.ops.Cacheable.SerItem
import eu.easyminer.rdf.index.{CompressedQuad, Index}
import eu.easyminer.rdf.utils.serialization.Serializer
import eu.easyminer.rdf.serialization.CompressedQuadSerialization._
import eu.easyminer.rdf.serialization.TripleItemSerialization._

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