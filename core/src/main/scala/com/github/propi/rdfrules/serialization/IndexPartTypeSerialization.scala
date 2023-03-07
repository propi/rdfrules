package com.github.propi.rdfrules.serialization

import com.github.propi.rdfrules.index.Index.PartType
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

object IndexPartTypeSerialization {

  implicit val partTypeSerializer: Serializer[PartType] = {
    case PartType.Train => Array[Byte](0)
    case PartType.Test => Array[Byte](1)
  }

  implicit val partTypeDeserializer: Deserializer[PartType] = (v: Array[Byte]) => if (v.headOption.contains(1)) PartType.Test else PartType.Train

}