package com.github.propi.rdfrules.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.github.propi.rdfrules.index.IndexItem
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
object IndexItemSerialization {

  private def serializeTriple(s: Int, p: Int, o: Int): ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(s))
    baos.write(Serializer.serialize(p))
    baos.write(Serializer.serialize(o))
    baos
  }

  implicit val compressedTripleSerializer: Serializer[IndexItem.IntTriple] = (v: IndexItem.IntTriple) => {
    val baos = serializeTriple(v.s, v.p, v.o)
    baos.toByteArray
  }

  implicit val compressedTripleDeserializer: Deserializer[IndexItem.IntTriple] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    IndexItem.Triple(
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais)
    )
  }

  implicit val compressedQuadSerializer: Serializer[IndexItem.IntQuad] = (v: IndexItem.IntQuad) => {
    val baos = serializeTriple(v.s, v.p, v.o)
    baos.write(Serializer.serialize(v.g))
    baos.toByteArray
  }

  implicit val compressedQuadDeserializer: Deserializer[IndexItem.IntQuad] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    IndexItem.Quad(
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais)
    )
  }

  implicit val compressedQuadSerializationSize: SerializationSize[IndexItem.IntQuad] = new SerializationSize[IndexItem.IntQuad] {
    val size: Int = implicitly[SerializationSize[Int]].size * 4
  }

  implicit val compressedTripleSerializationSize: SerializationSize[IndexItem.IntTriple] = new SerializationSize[IndexItem.IntTriple] {
    val size: Int = implicitly[SerializationSize[Int]].size * 3
  }

}