package eu.easyminer.rdf.data

import eu.easyminer.rdf.utils.serialization.{Deserializer, Serializer}

/**
  * Created by Vaclav Zeman on 9. 10. 2017.
  */
object PrefixSerialization {

  implicit val prefixSerializer: Serializer[Prefix] = (v: Prefix) => implicitly[Serializer[(String, String)]].serialize(v.prefix -> v.nameSpace)

  implicit val prefixDeserializer: Deserializer[Prefix] = (v: Array[Byte]) => {
    val x = implicitly[Deserializer[(String, String)]].deserialize(v)
    Prefix(x._1, x._2)
  }

}
