package com.github.propi.rdfrules.data

import java.io._
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.data.ops._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.{Debugger, ForEach}
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Graph private(val name: TripleItem.Uri, val triples: TripleTraversableView, val userDefinedPrefixes: ForEach[Prefix])
  extends Transformable[Triple, Graph]
    with TriplesOps
    with QuadsOps[Graph]
    with PrefixesOps[Graph]
    with Discretizable[Graph]
    with Cacheable[Triple, Graph]
    with Debugable[Triple, Graph]
    with Sampleable[Triple, Graph] {

  protected val serializer: Serializer[Triple] = implicitly[Serializer[Triple]]
  protected val deserializer: Deserializer[Triple] = implicitly[Deserializer[Triple]]
  protected val serializationSize: SerializationSize[Triple] = implicitly[SerializationSize[Triple]]
  protected val dataLoadingText: String = "Graph loading"

  protected def coll: TripleTraversableView = triples

  protected def cachedTransform(col: TripleTraversableView): Graph = new Graph(name, col, userDefinedPrefixes)

  protected def transform(col: TripleTraversableView): Graph = new Graph(name, col, userDefinedPrefixes)

  protected def transformQuads(col: QuadTraversableView): Graph = transform(col.map(_.triple))

  protected def transformPrefixesAndColl(prefixes: ForEach[Prefix], col: QuadTraversableView): Graph = new Graph(name, col.map(_.triple), prefixes)

  protected def valueClassTag: ClassTag[Triple] = implicitly[ClassTag[Triple]]

  override protected def samplingDistributor: Option[Triple => Any] = Some(_.predicate)

  def foreach(f: Triple => Unit): Unit = triples.foreach(f)

  def `export`(os: => OutputStream)(implicit writer: RdfWriter): Unit = writer.writeToOutputStream(this, os)

  def `export`(file: File)(implicit writer: RdfWriter): Unit = {
    val newWriter = if (writer == RdfWriter.NoWriter) RdfWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: RdfWriter): Unit = `export`(new File(file))

  def quads: QuadTraversableView = triples.map(_.toQuad(name))

  def withName(name: TripleItem.Uri): Graph = new Graph(name, triples, userDefinedPrefixes)

  def toDataset: Dataset = Dataset(this)

  def mine(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer()))(implicit debugger: Debugger = Debugger.EmptyDebugger): Ruleset = toDataset.mine(miner, ruleConsumer)

  def index(implicit debugger: Debugger = Debugger.EmptyDebugger): Index = toDataset.index()

}

object Graph {

  val default: TripleItem.Uri = TripleItem.LongUri("")

  def apply(triples: TripleTraversableView): Graph = new Graph(default, triples, ForEach.empty)

  def apply(name: TripleItem.Uri, triples: TripleTraversableView): Graph = new Graph(name, triples, ForEach.empty)

  def apply(name: TripleItem.Uri, is: => InputStream)(implicit reader: RdfReader): Graph = new Graph(name, reader.fromInputStream(is).map(_.triple), ForEach.empty)

  def apply(name: TripleItem.Uri, file: File)(implicit reader: RdfReader): Graph = {
    val newReader = if (reader == RdfReader.NoReader) RdfReader(file) else reader
    new Graph(name, newReader.fromFile(file).map(_.triple), ForEach.empty)
  }

  def apply(name: TripleItem.Uri, file: String)(implicit reader: RdfReader): Graph = apply(name, new File(file))

  def apply(file: File)(implicit reader: RdfReader): Graph = apply(default, file)

  def apply(file: String)(implicit reader: RdfReader): Graph = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: RdfReader): Graph = apply(default, is)

  def fromCache(name: TripleItem.Uri, is: => InputStream): Graph = new Graph(
    name,
    (f: Triple => Unit) => {
      Deserializer.deserializeFromInputStream[Triple, Unit](is) { reader =>
        Iterator.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
      }
    },
    ForEach.empty
  )

  def fromCache(is: => InputStream): Graph = fromCache(default, is)

  def fromCache(name: TripleItem.Uri, file: File): Graph = fromCache(name, new FileInputStream(file))

  def fromCache(name: TripleItem.Uri, file: String): Graph = fromCache(name, new File(file))

  def fromCache(file: File): Graph = fromCache(default, file)

  def fromCache(file: String): Graph = fromCache(default, new File(file))

}