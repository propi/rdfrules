package eu.easyminer.rdf.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import eu.easyminer.rdf.index.CompressedQuad
import eu.easyminer.rdf.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
object CompressedQuadSerialization {

  implicit val compressedQuadSerializer: Serializer[CompressedQuad] = (v: CompressedQuad) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v.subject))
    baos.write(Serializer.serialize(v.predicate))
    baos.write(Serializer.serialize(v.`object`))
    baos.write(Serializer.serialize(v.graph))
    baos.toByteArray
  }

  implicit val compressedQuadDeserializer: Deserializer[CompressedQuad] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    CompressedQuad(
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais),
      Deserializer.deserialize[Int](bais)
    )
  }

  implicit val compressedQuadSerializationSize: SerializationSize[CompressedQuad] = new SerializationSize[CompressedQuad] {
    val size: Int = implicitly[SerializationSize[Int]].size * 4
  }

}