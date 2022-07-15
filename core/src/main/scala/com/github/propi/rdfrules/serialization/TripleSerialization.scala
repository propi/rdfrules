package com.github.propi.rdfrules.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.prediction.PredictedTriple
import com.github.propi.rdfrules.rule.InstantiatedAtom
import com.github.propi.rdfrules.rule.InstantiatedRule.PredictedResult
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

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

  implicit val predictedTripleSerializer: Serializer[PredictedTriple] = (v: PredictedTriple) => {
    Serializer.serialize((v.triple, v.predictedResult, v.rules.iterator))
  }

  implicit val predictedTripleDeserializer: Deserializer[PredictedTriple] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (triple, predictedResult, rules) = Deserializer.deserialize[(InstantiatedAtom, PredictedResult, Iterable[FinalRule])](bais)
    PredictedTriple(triple, predictedResult, rules.toSet)
  }

}
