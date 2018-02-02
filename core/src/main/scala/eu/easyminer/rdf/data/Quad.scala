package eu.easyminer.rdf.data

import org.apache.jena.sparql.core

import scala.collection.TraversableView
import eu.easyminer.rdf.utils.extensions.TraversableOnceExtension._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
case class Quad(triple: Triple, graph: TripleItem.Uri)

object Quad {

  type QuadTraversableView = TraversableView[Quad, Traversable[_]]

  def apply(triple: Triple): Quad = new Quad(triple, TripleItem.LongUri(""))

  implicit def quadToJenaQuad(quad: Quad): core.Quad = new core.Quad(quad.graph, quad.triple)

  implicit class PimpedQuads(col: QuadTraversableView) {
    def prefixes: Traversable[Prefix] = col.flatMap(x => Iterator(x.graph, x.triple.subject, x.triple.predicate, x.triple.`object`)).collect {
      case x: TripleItem.PrefixedUri => x.toPrefix
    }.distinct
  }

}
