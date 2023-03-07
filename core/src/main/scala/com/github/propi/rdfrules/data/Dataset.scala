package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.data.ops._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.index.ops.TrainTestIndex
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.serialization.QuadSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

import java.io._
import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Dataset private(val quads: QuadTraversableView, val userDefinedPrefixes: ForEach[Prefix])
  extends Transformable[Quad, Dataset]
    with TriplesOps
    with QuadsOps[Dataset]
    with PrefixesOps[Dataset]
    with Discretizable[Dataset]
    with Cacheable[Quad, Dataset]
    with Debugable[Quad, Dataset]
    with Sampleable[Quad, Dataset] {

  protected val serializer: Serializer[Quad] = implicitly[Serializer[Quad]]
  protected val deserializer: Deserializer[Quad] = implicitly[Deserializer[Quad]]
  protected val serializationSize: SerializationSize[Quad] = implicitly[SerializationSize[Quad]]
  protected val dataLoadingText: String = "Dataset loading"

  protected def coll: QuadTraversableView = quads

  protected def cachedTransform(col: QuadTraversableView): Dataset = new Dataset(col, userDefinedPrefixes)

  protected def transform(col: QuadTraversableView): Dataset = new Dataset(col, userDefinedPrefixes)

  protected def transformQuads(col: QuadTraversableView): Dataset = transform(col)

  protected def transformPrefixesAndColl(prefixes: ForEach[Prefix], col: QuadTraversableView): Dataset = new Dataset(col, prefixes)

  protected def valueClassTag: ClassTag[Quad] = implicitly[ClassTag[Quad]]

  override protected def samplingDistributor: Option[Quad => Any] = Some(_.triple.predicate)

  def +(graph: Graph): Dataset = new Dataset(quads.concat(graph.quads), userDefinedPrefixes).addPrefixes(graph.userDefinedPrefixes)

  def +(dataset: Dataset): Dataset = new Dataset(quads.concat(dataset.quads), userDefinedPrefixes).addPrefixes(dataset.userDefinedPrefixes)

  def triples: TripleTraversableView = quads.map(_.triple)

  def toGraphs: ForEach[Graph] = quads.map(_.graph).distinct.map(x => Graph(x, quads.filter(_.graph == x).map(_.triple)).setPrefixes(userDefinedPrefixes))

  def foreach(f: Quad => Unit): Unit = quads.foreach(f)

  def `export`(os: => OutputStream)(implicit writer: RdfWriter): Unit = writer.writeToOutputStream(this, os)

  def `export`(file: File)(implicit writer: RdfWriter): Unit = {
    val newWriter = if (writer == RdfWriter.NoWriter) RdfWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: RdfWriter): Unit = `export`(new File(file))

  def mine(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer()))(implicit debugger: Debugger): Ruleset = index.mineRules(miner, ruleConsumer)

  def index(implicit debugger: Debugger): Index = Index(this, false)

  def index(train: Set[TripleItem.Uri], test: Set[TripleItem.Uri])(implicit debugger: Debugger): TrainTestIndex = {
    val testCache = collection.mutable.ArrayBuffer.empty[Quad]
    val trainDataset = transform((f: Quad => Unit) => {
      testCache.clear()
      for (quad <- quads) {
        if (train(quad.graph)) f(quad) else if (test(quad.graph)) testCache.addOne(quad)
      }
    })
    TrainTestIndex(trainDataset, Dataset(new ForEach[Quad] {
      def foreach(f: Quad => Unit): Unit = testCache.foreach(f)

      override def knownSize: Int = testCache.length
    }), false)
  }

}

object Dataset {

  def apply(graph: Graph): Dataset = new Dataset(graph.quads, graph.userDefinedPrefixes)

  def apply(): Dataset = new Dataset(ForEach.empty, ForEach.empty)

  def apply(is: => InputStream)(implicit reader: RdfReader): Dataset = new Dataset(reader.fromInputStream(is), ForEach.empty)

  def apply(file: File)(implicit reader: RdfReader): Dataset = {
    val newReader = if (reader == RdfReader.NoReader) RdfReader(file) else reader
    new Dataset(newReader.fromFile(file), ForEach.empty)
  }

  def apply(file: String)(implicit reader: RdfReader): Dataset = apply(new File(file))

  def apply(quads: QuadTraversableView): Dataset = new Dataset(quads, ForEach.empty)

  def fromCache(is: => InputStream): Dataset = new Dataset(
    (f: Quad => Unit) => {
      Deserializer.deserializeFromInputStream[Quad, Unit](is) { reader =>
        Iterator.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
      }
    },
    ForEach.empty
  )

  def fromCache(file: File): Dataset = fromCache(new FileInputStream(file))

  def fromCache(file: String): Dataset = fromCache(new File(file))

}