package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.index.{IndexItem, TripleItemIndex}
import com.github.propi.rdfrules.utils.{ForEach, Stringifier}
import org.apache.jena.graph.Node
import org.apache.jena.sparql.core

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
case class Quad private(triple: Triple, graph: TripleItem.Uri) {
  override def toString: String = Stringifier(this)

  def intern: Quad = new Quad(triple.intern, graph.intern)

  //def copy(triple: Triple = triple, graph: TripleItem.Uri = graph): Quad = Quad(triple, graph)
}

object Quad {

  type QuadTraversableView = ForEach[Quad]

  def apply(triple: Triple, graph: TripleItem.Uri): Quad = new Quad(triple, graph)

  def apply(triple: Triple): Quad = new Quad(triple, Graph.default)

  implicit def quadToJenaQuad(quad: Quad): core.Quad = {
    val graphNode: Node = if (quad.graph.hasSameUriAs(Graph.default)) core.Quad.defaultGraphIRI else quad.graph
    new core.Quad(graphNode, quad.triple)
  }

  implicit class PimpedQuads(val col: QuadTraversableView) extends AnyVal {
    def prefixes: ForEach[Prefix] = col.flatMap(x => ForEach(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).collect {
      case x: TripleItem.PrefixedUri => x.prefix
    }.distinct
  }

  implicit class PimpedQuad(x: Quad)(implicit mapper: TripleItemIndex) {
    def toCompressedQuad: IndexItem.IntQuad = IndexItem.Quad(
      mapper.getIndex(x.triple.subject),
      mapper.getIndex(x.triple.predicate),
      mapper.getIndex(x.triple.`object`),
      mapper.getIndex(x.graph)
    )
  }

  implicit val quadStringifier: Stringifier[Quad] = (v: Quad) => Stringifier(v.triple) + " " + v.graph

}
