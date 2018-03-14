package eu.easyminer.rdf.data

import eu.easyminer.rdf.index.{CompressedQuad, TripleItemHashIndex}
import eu.easyminer.rdf.utils.extensions.TraversableOnceExtension._
import org.apache.jena.graph.Node
import org.apache.jena.sparql.core

import scala.collection.TraversableView
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
case class Quad(triple: Triple, graph: TripleItem.Uri)

object Quad {

  type QuadTraversableView = TraversableView[Quad, Traversable[_]]

  def apply(triple: Triple): Quad = new Quad(triple, Graph.default)

  implicit def quadToJenaQuad(quad: Quad): core.Quad = {
    val graphNode: Node = if (quad.graph.hasSameUriAs(Graph.default)) core.Quad.defaultGraphIRI else quad.graph
    new core.Quad(graphNode, quad.triple)
  }

  implicit class PimpedQuads(col: QuadTraversableView) {
    def prefixes: Traversable[Prefix] = col.flatMap(x => Iterator(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).collect {
      case x: TripleItem.PrefixedUri => x.toPrefix
    }.distinct
  }

  implicit class PimpedQuad(x: Quad)(implicit mapper: TripleItemHashIndex) {
    def toCompressedQuad: CompressedQuad = CompressedQuad(
      mapper.getIndex(x.triple.subject),
      mapper.getIndex(x.triple.predicate),
      mapper.getIndex(x.triple.`object`),
      mapper.getIndex(x.graph)
    )
  }

}
