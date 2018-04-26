package com.github.propi.rdfrules.serialization

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

import com.github.propi.rdfrules.rule.{Atom, Measure, Rule}
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.Serializer._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.language.implicitConversions

object RuleSerialization {

  implicit private def atomItemToInt(item: Atom.Item): Int = item match {
    case x: Atom.Variable => x.index
    case x: Atom.Constant => x.value
  }

  /**
    * Bytes:
    * - 4: predicate int
    * - 1: true/false - is subject variable
    * - 4: subject int
    * - 1: true/false - is object variable
    * - 4: object int
    */
  implicit val atomSerializationSize: SerializationSize[Atom] = new SerializationSize[Atom] {
    val size: Int = 14
  }

  implicit val atomSerializer: Serializer[Atom] = (v: Atom) => {
    ByteBuffer.allocate(atomSerializationSize.size)
      .put(Serializer.serialize(v.predicate))
      .put(Serializer.serialize(v.subject.isInstanceOf[Atom.Variable]))
      .put(Serializer.serialize[Int](v.subject))
      .put(Serializer.serialize(v.`object`.isInstanceOf[Atom.Variable]))
      .put(Serializer.serialize[Int](v.`object`))
      .array()
  }

  implicit val atomDeserializer: Deserializer[Atom] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val predicate = Deserializer.deserialize[Int](bais)
    val subject: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
    val `object`: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
    Atom(subject, predicate, `object`)
  }

  /**
    * Bytes:
    * - 1: byte - type
    * - 8: double - value
    */
  implicit val measureSerializationSize: SerializationSize[Measure] = new SerializationSize[Measure] {
    val size: Int = 9
  }

  implicit val measureSerializer: Serializer[Measure] = (v: Measure) => {
    val buffer = ByteBuffer.allocate(measureSerializationSize.size)
    val (mtype, value) = v match {
      case Measure.BodySize(x) => 1 -> x.toDouble
      case Measure.Confidence(x) => 2 -> x
      case Measure.HeadConfidence(x) => 3 -> x
      case Measure.HeadCoverage(x) => 4 -> x
      case Measure.HeadSize(x) => 5 -> x.toDouble
      case Measure.Lift(x) => 6 -> x
      case Measure.PcaBodySize(x) => 7 -> x.toDouble
      case Measure.PcaConfidence(x) => 8 -> x
      case Measure.PcaLift(x) => 9 -> x
      case Measure.Support(x) => 10 -> x.toDouble
      case Measure.Cluster(x) => 11 -> x.toDouble
    }
    buffer.put(mtype.toByte)
    buffer.putDouble(value)
    buffer.array()
  }

  implicit val measureDeserializer: Deserializer[Measure] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val mtype = bais.read()
    val value = Deserializer.deserialize[Double](bais)
    mtype match {
      case 1 => Measure.BodySize(value.toInt)
      case 2 => Measure.Confidence(value)
      case 3 => Measure.HeadConfidence(value)
      case 4 => Measure.HeadCoverage(value)
      case 5 => Measure.HeadSize(value.toInt)
      case 6 => Measure.Lift(value)
      case 7 => Measure.PcaBodySize(value.toInt)
      case 8 => Measure.PcaConfidence(value)
      case 9 => Measure.PcaLift(value)
      case 10 => Measure.Support(value.toInt)
      case 11 => Measure.Cluster(value.toInt)
      case _ => throw new Deserializer.DeserializationException("Invalid type of a measure.")
    }
  }

  implicit val ruleSerializer: Serializer[Rule] = (v: Rule) => {
    val measuresBytes = Serializer.serialize(v.measures.iterator.toTraversable)
    val atomsBytes = Serializer.serialize((v.head +: v.body).asInstanceOf[Traversable[Atom]])
    measuresBytes ++ atomsBytes
  }

  implicit val ruleDeserializer: Deserializer[Rule] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val measures = TypedKeyMap(Deserializer.deserialize[Traversable[Measure]](bais).map(x => x: (Key[Measure], Measure)).toSeq: _*)
    val atoms = Deserializer.deserialize[Traversable[Atom]](bais)
    Rule.Simple(atoms.head, atoms.tail.toIndexedSeq)(measures)
  }

  implicit val ruleSimpleSerializer: Serializer[Rule.Simple] = (v: Rule.Simple) => ruleSerializer.serialize(v)

  implicit val ruleSimpleDeserializer: Deserializer[Rule.Simple] = (v: Array[Byte]) => ruleDeserializer.deserialize(v).asInstanceOf[Rule.Simple]

}