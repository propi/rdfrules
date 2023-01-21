package com.github.propi.rdfrules.serialization

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.prediction.PredictedResult
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem
import com.github.propi.rdfrules.rule.{Measure, ResolvedAtom, ResolvedInstantiatedAtom, ResolvedInstantiatedRule, ResolvedRule}
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.Serializer._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import scala.language.implicitConversions

object RuleSerialization {

  /*implicit private def atomItemToInt(item: Atom.Item): Int = item match {
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
  private val atomBasicSerializationSize: Int = 14

  implicit val atomGraphBasedSerializer: Serializer[Atom.GraphAware] = (v: Atom.GraphAware) => {
    Serializer.serialize(Atom(v.subject, v.predicate, v.`object`) -> v.graphsIterator)
  }

  implicit val atomGraphBasedDeserializer: Deserializer[Atom.GraphAware] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (atom, graphs) = Deserializer.deserialize[(Atom, Iterable[Int])](bais)
    val graphsSet: MutableHashSet[Int] = new MutableHashSet[Int] {
      private val hset = new IntOpenHashSet()

      def +=(x: Int): Unit = hset.add(x)

      def -=(x: Int): Unit = hset.remove(x)

      def iterator: Iterator[Int] = hset.iterator().asScala.asInstanceOf[Iterator[Int]]

      def contains(x: Int): Boolean = hset.contains(x)

      def size: Int = hset.size()

      def trim(): Unit = hset.trim()

      def isEmpty: Boolean = hset.isEmpty
    }
    graphs.foreach(graphsSet += _)
    Atom(atom.subject, atom.predicate, atom.`object`, graphsSet)
  }

  implicit val atomSerializer: Serializer[Atom] = {
    case x: Atom.GraphAware => Serializer.serialize(x)
    case x =>
      ByteBuffer.allocate(atomBasicSerializationSize)
        .put(Serializer.serialize(x.predicate))
        .put(Serializer.serialize(x.subject.isInstanceOf[Atom.Variable]))
        .put(Serializer.serialize[Int](x.subject))
        .put(Serializer.serialize(x.`object`.isInstanceOf[Atom.Variable]))
        .put(Serializer.serialize[Int](x.`object`))
        .array()
  }

  implicit val atomDeserializer: Deserializer[Atom] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    if (v.length == atomBasicSerializationSize) {
      val predicate = Deserializer.deserialize[Int](bais)
      val subject: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
      val `object`: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
      Atom(subject, predicate, `object`)
    } else {
      Deserializer.deserialize[Atom.GraphAware](bais)
    }
  }*/

  implicit val resolvedAtomItemSerializer: Serializer[ResolvedItem] = (v: ResolvedItem) => {
    val baos = new ByteArrayOutputStream()
    v match {
      case ResolvedItem.Variable(v) =>
        baos.write(1)
        baos.write(Serializer.serialize(v))
      case ResolvedItem.Constant(c) =>
        baos.write(2)
        baos.write(Serializer.serialize(c))
    }
    baos.toByteArray
  }

  implicit val resolvedAtomItemDeserializer: Deserializer[ResolvedItem] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    bais.read() match {
      case 1 => ResolvedItem.Variable(Deserializer.deserialize[String](bais))
      case 2 => ResolvedItem.Constant(Deserializer.deserialize[TripleItem](bais))
      case x => throw new Deserializer.DeserializationException("No deserializer for index: " + x)
    }
  }

  implicit val resolvedAtomGraphBasedSerializer: Serializer[ResolvedAtom.GraphAware] = (v: ResolvedAtom.GraphAware) => {
    Serializer.serialize(ResolvedAtom(v.subject, v.predicate, v.`object`) -> v.graphs.iterator)
  }

  implicit val resolvedAtomGraphBasedDeserializer: Deserializer[ResolvedAtom.GraphAware] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (atom, graphs) = Deserializer.deserialize[(ResolvedAtom, Iterable[TripleItem.Uri])](bais)
    ResolvedAtom(atom.subject, atom.predicate, atom.`object`, graphs.toSet)
  }

  implicit val resolvedAtomSerializer: Serializer[ResolvedAtom] = (v: ResolvedAtom) => {
    val baos = new ByteArrayOutputStream()
    v match {
      case x: ResolvedAtom.GraphAware =>
        baos.write(2)
        baos.write(Serializer.serialize(x))
      case _ =>
        baos.write(1)
        baos.write(Serializer.serialize((v.subject, v.predicate, v.`object`)))
    }
    baos.toByteArray
  }

  implicit val resolvedAtomDeserializer: Deserializer[ResolvedAtom] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    bais.read() match {
      case 2 => Deserializer.deserialize[ResolvedAtom.GraphAware](bais)
      case 1 =>
        val (s, p, o) = Deserializer.deserialize[(ResolvedItem, TripleItem.Uri, ResolvedItem)](bais)
        ResolvedAtom(s, p, o)
      case x => throw new Deserializer.DeserializationException("No deserializer for index: " + x)
    }
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
      case Measure.CwaConfidence(x) => 2 -> x
      case Measure.HeadCoverage(x) => 4 -> x
      case Measure.HeadSize(x) => 5 -> x.toDouble
      case Measure.Lift(x) => 6 -> x
      case Measure.PcaBodySize(x) => 7 -> x.toDouble
      case Measure.PcaConfidence(x) => 8 -> x
      case Measure.Support(x) => 9 -> x.toDouble
      case Measure.Cluster(x) => 10 -> x.toDouble
      case Measure.SupportIncreaseRatio(x) => 11 -> x.toDouble
      case Measure.HeadSupport(x) => 12 -> x.toDouble
      case Measure.QpcaBodySize(x) => 13 -> x.toDouble
      case Measure.QpcaConfidence(x) => 14 -> x
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
      case 2 => Measure.CwaConfidence(value)
      case 4 => Measure.HeadCoverage(value)
      case 5 => Measure.HeadSize(value.toInt)
      case 6 => Measure.Lift(value)
      case 7 => Measure.PcaBodySize(value.toInt)
      case 8 => Measure.PcaConfidence(value)
      case 9 => Measure.Support(value.toInt)
      case 10 => Measure.Cluster(value.toInt)
      case 11 => Measure.SupportIncreaseRatio(value.toFloat)
      case 12 => Measure.HeadSupport(value.toInt)
      case 13 => Measure.QpcaBodySize(value.toInt)
      case 14 => Measure.QpcaConfidence(value)
      case _ => throw new Deserializer.DeserializationException("Invalid type of a measure.")
    }
  }

  implicit val predictionResultSerializationSize: SerializationSize[PredictedResult] = new SerializationSize[PredictedResult] {
    val size: Int = 1
  }

  implicit val predictionResultSerializer: Serializer[PredictedResult] = (v: PredictedResult) => {
    val buffer = ByteBuffer.allocate(predictionResultSerializationSize.size)
    val s = v match {
      case PredictedResult.Positive => 1
      case PredictedResult.Negative => 2
      case PredictedResult.PcaPositive => 3
    }
    buffer.put(s.toByte).array()
  }

  implicit val predictionResultDeserializer: Deserializer[PredictedResult] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    bais.read() match {
      case 1 => PredictedResult.Positive
      case 2 => PredictedResult.Negative
      case 3 => PredictedResult.PcaPositive
      case _ => throw new Deserializer.DeserializationException("Invalid type of a prediction result.")
    }
  }

  /*implicit val instantiatedAtomSerializationSize: SerializationSize[InstantiatedAtom] = new SerializationSize[InstantiatedAtom] {
    val size: Int = 12
  }

  implicit val instantiatedAtomSerializer: Serializer[InstantiatedAtom] = (v: InstantiatedAtom) => {
    ByteBuffer.allocate(instantiatedAtomSerializationSize.size)
      .put(Serializer.serialize(v.subject))
      .put(Serializer.serialize(v.predicate))
      .put(Serializer.serialize(v.`object`))
      .array()
  }

  implicit val instantiatedAtomDeserializer: Deserializer[InstantiatedAtom] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val s = Deserializer.deserialize[Int](bais)
    val p = Deserializer.deserialize[Int](bais)
    val o = Deserializer.deserialize[Int](bais)
    InstantiatedAtom(s, p, o)
  }

  implicit val ruleSerializer: Serializer[Rule] = (v: Rule) => {
    val measuresBytes = Serializer.serialize(v.measures.iterator)
    val atomsBytes = Serializer.serialize((v.head +: v.body).iterator)
    measuresBytes ++ atomsBytes
  }

  implicit val ruleDeserializer: Deserializer[Rule] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val measures = TypedKeyMap(Deserializer.deserialize[Iterable[Measure]](bais).map(x => x: (Key[Measure], Measure)).toSeq: _*)
    val atoms = Deserializer.deserialize[Iterable[Atom]](bais)
    Rule(atoms.head, atoms.tail.toIndexedSeq)(measures)
  }

  implicit val ruleSimpleSerializer: Serializer[FinalRule] = (v: FinalRule) => ruleSerializer.serialize(v)

  implicit val ruleSimpleDeserializer: Deserializer[FinalRule] = (v: Array[Byte]) => ruleDeserializer.deserialize(v).asInstanceOf[FinalRule]*/

  implicit val resolvedRuleSerializer: Serializer[ResolvedRule] = (v: ResolvedRule) => {
    val measuresBytes = Serializer.serialize(v.measures.iterator)
    val atomsBytes = Serializer.serialize((v.head +: v.body).iterator)
    measuresBytes ++ atomsBytes
  }

  implicit val resolvedRuleDeserializer: Deserializer[ResolvedRule] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val measures = TypedKeyMap(Deserializer.deserialize[Iterable[Measure]](bais))
    val atoms = Deserializer.deserialize[Iterable[ResolvedAtom]](bais)
    ResolvedRule(atoms.tail.toIndexedSeq, atoms.head)(measures)
  }

  implicit val resolvedInstantiatedAtomSerializer: Serializer[ResolvedInstantiatedAtom] = (v: ResolvedInstantiatedAtom) => {
    Serializer.serialize((v.subject, v.predicate, v.`object`))
  }

  implicit val resolvedInstantiatedAtomDeserializer: Deserializer[ResolvedInstantiatedAtom] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (s, p, o) = Deserializer.deserialize[(TripleItem.Uri, TripleItem.Uri, TripleItem)](bais)
    ResolvedInstantiatedAtom(s, p, o)
  }

  implicit val resolvedInstantiatedRuleSerializer: Serializer[ResolvedInstantiatedRule] = (v: ResolvedInstantiatedRule) => {
    Serializer.serialize(((v.head +: v.body).iterator, v.source, v.predictedResult))
  }

  implicit val resolvedInstantiatedRuleDeserializer: Deserializer[ResolvedInstantiatedRule] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (atoms, source, predictionResult) = Deserializer.deserialize[(Iterable[ResolvedInstantiatedAtom], ResolvedRule, PredictedResult)](bais)
    ResolvedInstantiatedRule(atoms.head, atoms.tail.toIndexedSeq, predictionResult, source)
  }

}