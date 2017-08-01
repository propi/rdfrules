package eu.easyminer.rdf.rule

import java.nio.ByteBuffer

import eu.easyminer.rdf.rule.Measure.Measures
import eu.easyminer.rdf.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
object RuleSerialization {

  implicit private class PimpedBoolean(v: Boolean) {
    def toByte: Byte = if (v) 1 else 0
  }

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
  implicit val atomSerializationSize = new SerializationSize[Atom] {
    val size: Int = 14
  }

  implicit val atomSerializer = new Serializer[Atom] {
    def serialize(v: Atom): Array[Byte] = ByteBuffer.allocate(atomSerializationSize.size)
      .putInt(v.predicate)
      .put(v.subject.isInstanceOf[Atom.Variable].toByte)
      .putInt(v.subject)
      .put(v.subject.isInstanceOf[Atom.Variable].toByte)
      .putInt(v.`object`)
      .array()
  }

  implicit val atomDeserializer = new Deserializer[Atom] {
    def deserialize(v: Array[Byte]): Atom = {
      val buffer = ByteBuffer.wrap(v)
      val predicate = buffer.getInt
      val subject: Atom.Item = if (buffer.get() == 1) Atom.Variable(buffer.getInt) else Atom.Constant(buffer.getInt)
      val `object`: Atom.Item = if (buffer.get() == 1) Atom.Variable(buffer.getInt) else Atom.Constant(buffer.getInt)
      Atom(subject, predicate, `object`)
    }
  }

  /**
    * Bytes:
    * - 4: int - head size
    * - 4: int - body size
    * - 4: int - support
    * - 8: double - head coverage
    * - 8: double - confidence
    */
  implicit val measuresSerializationSize = new SerializationSize[Measures] {
    val size: Int = 28
  }

  implicit val measuresSerializer = new Serializer[Measures] {
    def serialize(v: Measures): Array[Byte] = ByteBuffer.allocate(measuresSerializationSize.size)
      .putInt(v(Measure.HeadSize).asInstanceOf[Measure.HeadSize].value)
      .putInt(v(Measure.BodySize).asInstanceOf[Measure.BodySize].value)
      .putInt(v(Measure.Support).asInstanceOf[Measure.Support].value)
      .putDouble(v(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value)
      .putDouble(v(Measure.Confidence).asInstanceOf[Measure.Confidence].value)
      .array()
  }

  implicit val measuresDeserializer = new Deserializer[Measures] {
    def deserialize(v: Array[Byte]): Measures = {
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

  implicit val ruleSerializer = new Serializer[Rule] {
    def serialize(v: Rule): Array[Byte] = {
      val atomsBytes = Serializer.serialize(v.head +: v.body)
      val measuresBytes = Serializer.serialize(v.measures)
      measuresBytes ++ atomsBytes
    }
  }

  implicit val ruleDeserializer = new Deserializer[Rule] {
    def deserialize(v: Array[Byte]): Rule = {
      val measures = Deserializer.deserialize[Measures](v.take(measuresSerializationSize.size))
      val atoms = Deserializer.deserialize[Traversable[Atom]](v.drop(measuresSerializationSize.size))
      Rule.Simple(atoms.head, atoms.tail.asInstanceOf[IndexedSeq[Atom]])(measures)
    }
  }

}
