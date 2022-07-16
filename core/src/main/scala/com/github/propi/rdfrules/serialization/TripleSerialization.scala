package com.github.propi.rdfrules.serialization

import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.prediction.{PredictedResult, ResolvedPredictedTriple}
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleSerialization {

  implicit val tripleSerializer: Serializer[data.Triple] = (v: data.Triple) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v.subject))
    baos.write(Serializer.serialize(v.predicate))
    baos.write(Serializer.serialize(v.`object`))
    baos.toByteArray
  }

  implicit val tripleDeserializer: Deserializer[data.Triple] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val s = Deserializer.deserialize[TripleItem.Uri](bais)
    val p = Deserializer.deserialize[TripleItem.Uri](bais)
    val o = Deserializer.deserialize[TripleItem](bais)
    data.Triple(s, p, o)
  }

  implicit val resolvedPredictedTripleSerializer: Serializer[ResolvedPredictedTriple] = (v: ResolvedPredictedTriple) => {
    Serializer.serialize((v.triple, v.predictedResult, v.rules.iterator))
  }

  implicit val resolvedPredictedTripleDeserializer: Deserializer[ResolvedPredictedTriple] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (triple, predictedResult, rules) = Deserializer.deserialize[(Triple, PredictedResult, Iterable[ResolvedRule])](bais)
    ResolvedPredictedTriple(triple, predictedResult, rules.toSet)
  }

}
