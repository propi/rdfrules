package eu.easyminer.rdf.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import eu.easyminer.rdf.data.{Quad, Triple, TripleItem}
import eu.easyminer.rdf.utils.serialization.{Deserializer, Serializer}
import TripleSerialization._
import TripleItemSerialization._

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
object QuadSerialization {

  implicit val quadSerializer: Serializer[Quad] = (v: Quad) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v.graph))
    baos.write(Serializer.serialize(v.triple))
    baos.toByteArray
  }

  implicit val quadDeserializer: Deserializer[Quad] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val g = Deserializer.deserialize[TripleItem.Uri](bais)
    val t = Deserializer.deserialize[Triple](bais)
    Quad(t, g)
  }

}
