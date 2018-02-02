package eu.easyminer.rdf.data

import java.io.{File, InputStream, OutputStream}

import eu.easyminer.rdf.data.Quad.QuadTraversableView
import eu.easyminer.rdf.data.Triple.TripleTraversableView
import eu.easyminer.rdf.utils.extensions.TraversableOnceExtension._

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Dataset(quads: QuadTraversableView) extends Transformable[Quad, Dataset] with TriplesOps {

  /*def +(graph: Graph): Dataset = {
    var isAdded = false
    val updatedGraphs = graphs.map { oldGraph =>
      if (oldGraph.name.hasSameUriAs(graph.name)) {
        isAdded = true
        oldGraph.copy(triples = oldGraph.triples ++ graph.triples)
      } else {
        oldGraph
      }
    }
    new Dataset(if (isAdded) updatedGraphs else graphs :+ graph)
  }

  def -(graphName: String): Dataset = new Dataset(graphs.filter(_.name != graphName))

  def withPrefixes(prefixes: Seq[Prefix]): Dataset = {
    val map = prefixes.iterator.map(x => x.nameSpace -> x.prefix).toMap

    def uriToPrefixedUri(uri: TripleItem.Uri) = uri match {
      case x: TripleItem.LongUri =>
        val puri = x.toPrefixedUri
        map.get(puri.nameSpace).map(prefix => puri.copy(prefix = prefix)).getOrElse(x)
      case x => x
    }

    def tripleToTripleWithPrefixes(triple: Triple) = triple.copy(
      uriToPrefixedUri(triple.subject),
      uriToPrefixedUri(triple.predicate),
      triple.`object` match {
        case x: TripleItem.Uri => uriToPrefixedUri(x)
        case x => x
      }
    )

    new Dataset(
      graphs.map(graph => graph.copy(triples = graph.triples.map(tripleToTripleWithPrefixes)))
    )
  }

  def withFilter(f: Triple => Boolean): Dataset = new Dataset(graphs.map(graph => graph.copy(triples = graph.triples.filter(f))))

  def withReplace(f: Triple => Triple): Dataset = new Dataset(graphs.map(graph => graph.copy(triples = graph.triples.map(f))))*/

  protected def coll: TraversableView[Quad, Traversable[_]] = quads

  protected def transform(col: TraversableView[Quad, Traversable[_]]): Dataset = new Dataset(col)

  def +(graph: Graph): Dataset = new Dataset(quads ++ graph.toQuads)

  def +(dataset: Dataset): Dataset = new Dataset(quads ++ dataset.toQuads)

  def toQuads: QuadTraversableView = quads

  def toTriples: TripleTraversableView = quads.map(_.triple)

  def toGraphs: Traversable[Graph] = quads.map(_.graph).distinct.view.map(x => new Graph(x, quads.filter(_.graph == x).map(_.triple)))

  def foreach(f: Quad => Unit): Unit = quads.foreach(f)

  def prefixes: Traversable[Prefix] = quads.prefixes

  def export[T](os: => OutputStream)(implicit writer: RdfWriter[T]): Unit = writer.writeToOutputStream(this, os)
}

object Dataset {

  def apply(graph: Graph): Dataset = new Dataset(graph.toQuads)

  def apply(): Dataset = new Dataset(Traversable.empty[Quad].view)

  def apply[T](is: => InputStream)(implicit reader: RdfReader[T]): Dataset = new Dataset(reader.fromInputStream(is))

  def apply[T](file: File)(implicit reader: RdfReader[T]): Dataset = new Dataset(reader.fromFile(file))

}