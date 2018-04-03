package com.github.propi.rdfrules.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data.{Quad, Triple, TripleItem}
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

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
    data.Quad(t, g)
  }

}
