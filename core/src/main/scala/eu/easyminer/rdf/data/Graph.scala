package eu.easyminer.rdf.data

import java.io.{File, InputStream}

import eu.easyminer.rdf.data.Quad.QuadTraversableView
import eu.easyminer.rdf.data.Triple.TripleTraversableView

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
case class Graph(name: TripleItem.Uri, triples: TripleTraversableView)

object Graph {

  implicit private def quadsToGraphs(quads: QuadTraversableView): IndexedSeq[Graph] = quads.map(_.graph).toSet.toVector.map(graph => Graph(graph, quads.filter(_.graph == graph).map(_.triple)))

  def apply[T](is: => InputStream)(implicit reader: RdfReader[T]): IndexedSeq[Graph] = reader.fromInputStream(is)

  def apply[T](file: File)(implicit reader: RdfReader[T]): IndexedSeq[Graph] = reader.fromFile(file)

  def apply[T](name: TripleItem.Uri, is: => InputStream)(implicit reader: RdfReader[T]): IndexedSeq[Graph] = Vector(Graph(name, reader.fromInputStream(is).map(_.triple)))

  def apply[T](name: TripleItem.Uri, file: File)(implicit reader: RdfReader[T]): IndexedSeq[Graph] = Vector(Graph(name, reader.fromFile(file).map(_.triple)))

}