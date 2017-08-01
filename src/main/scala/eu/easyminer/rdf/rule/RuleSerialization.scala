package eu.easyminer.rdf.rule

import java.nio.ByteBuffer

import eu.easyminer.rdf.rule.Measure.Measures
import eu.easyminer.rdf.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.language.implicitConversions

trait RuleSerialization {

  implicit private class PimpedBoolean(v: Boolean) {
    def toByte: Byte = if (v) 1 else 0
  }

  implicit private def atomItemToInt(item: Atom.Item): Int = item match {
    case x: Atom.Variable => x.index
    case x: Atom.Constant => x.value
  }

  object AtomSerialization {

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

    implicit val atomSerializer: Serializer[Atom] = (v: Atom) => ByteBuffer.allocate(atomSerializationSize.size)
      .putInt(v.predicate)
      .put(v.subject.isInstanceOf[Atom.Variable].toByte)
      .putInt(v.subject)
      .put(v.`object`.isInstanceOf[Atom.Variable].toByte)
      .putInt(v.`object`)
      .array()

    implicit val atomDeserializer: Deserializer[Atom] = (v: Array[Byte]) => {
      val buffer = ByteBuffer.wrap(v)
      val predicate = buffer.getInt
      val subject: Atom.Item = if (buffer.get() == 1) Atom.Variable(buffer.getInt) else Atom.Constant(buffer.getInt)
      val `object`: Atom.Item = if (buffer.get() == 1) Atom.Variable(buffer.getInt) else Atom.Constant(buffer.getInt)
      Atom(subject, predicate, `object`)
    }

  }

  object MeasuresSerialization {

    /**
      * Bytes:
      * - 4: int - head size
      * - 4: int - body size
      * - 4: int - support
      * - 8: double - head coverage
      * - 8: double - confidence
      */
    implicit val measuresSerializationSize: SerializationSize[Measures] = new SerializationSize[Measures] {
      val size: Int = 28
    }

    implicit val measuresSerializer: Serializer[Measures] = (v: Measures) => ByteBuffer.allocate(measuresSerializationSize.size)
      .putInt(v(Measure.HeadSize).asInstanceOf[Measure.HeadSize].value)
      .putInt(v(Measure.BodySize).asInstanceOf[Measure.BodySize].value)
      .putInt(v(Measure.Support).asInstanceOf[Measure.Support].value)
      .putDouble(v(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value)
      .putDouble(v(Measure.Confidence).asInstanceOf[Measure.Confidence].value)
      .array()

    implicit val measuresDeserializer: Deserializer[Measures] = (v: Array[Byte]) => {
      val buffer = ByteBuffer.wrap(v)
      collection.mutable.Map(
        Measure.HeadSize(buffer.getInt),
        Measure.BodySize(buffer.getInt),
        Measure.Support(buffer.getInt),
        Measure.HeadCoverage(buffer.getDouble),
        Measure.Confidence(buffer.getDouble)
      )
    }

  }

  implicit val ruleSerializer: Serializer[Rule] = (v: Rule) => {
    import AtomSerialization._
    import MeasuresSerialization._
    val atomsBytes = Serializer.serialize(v.head +: v.body)
    val measuresBytes = Serializer.serialize(v.measures)
    measuresBytes ++ atomsBytes
  }

  implicit val ruleDeserializer: Deserializer[Rule] = (v: Array[Byte]) => {
    import AtomSerialization._
    import MeasuresSerialization._
    val measures = Deserializer.deserialize[Measures](v.take(measuresSerializationSize.size))
    val atoms = Deserializer.deserialize(v.drop(measuresSerializationSize.size))(Deserializer.traversableDeserializer[Atom])
    Rule.Simple(atoms.head, atoms.tail.asInstanceOf[IndexedSeq[Atom]])(measures)
  }

}

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
object RuleSerialization extends RuleSerialization
