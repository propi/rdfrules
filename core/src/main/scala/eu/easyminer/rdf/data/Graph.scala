package eu.easyminer.rdf.data

import java.io.{File, InputStream, OutputStream}

import eu.easyminer.rdf.data.Quad.QuadTraversableView
import eu.easyminer.rdf.data.Triple.TripleTraversableView

import scala.collection.TraversableView
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Graph(val name: TripleItem.Uri, triples: TripleTraversableView) extends Transformable[Triple, Graph] with TriplesOps {
  protected def coll: TraversableView[Triple, Traversable[_]] = triples

  protected def transform(col: TraversableView[Triple, Traversable[_]]): Graph = new Graph(name, col)

  def foreach(f: Triple => Unit): Unit = triples.foreach(f)

  def export[T](os: => OutputStream)(implicit writer: RdfWriter[T]): Unit = writer.writeToOutputStream(this, os)

  def toTriples: TripleTraversableView = triples

  def toQuads: QuadTraversableView = triples.map(_.toQuad(name))

  def prefixes: Traversable[Prefix] = toQuads.prefixes
}

object Graph {

  def apply[T](name: TripleItem.Uri, is: => InputStream)(implicit reader: RdfReader[T]): Graph = new Graph(name, reader.fromInputStream(is).map(_.triple))

  def apply[T](name: TripleItem.Uri, file: File)(implicit reader: RdfReader[T]): Graph = new Graph(name, reader.fromFile(file).map(_.triple))

}