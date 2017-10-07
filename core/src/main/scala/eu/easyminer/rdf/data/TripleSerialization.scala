package eu.easyminer.rdf.data

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import eu.easyminer.rdf.utils.serialization.{Deserializer, Serializer}

import TripleItemSerialization._

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleSerialization {

  implicit val tripleSerializer: Serializer[Triple] = (v: Triple) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v.subject))
    baos.write(Serializer.serialize(v.predicate))
    baos.write(Serializer.serialize(v.`object`))
    baos.toByteArray
  }

  implicit val tripleDeserializer: Deserializer[Triple] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val s = Deserializer.deserialize[TripleItem.Uri](bais)
    val p = Deserializer.deserialize[TripleItem.Uri](bais)
    val o = Deserializer.deserialize[TripleItem](bais)
    Triple(s, p, o)
  }

}
