package com.github.propi.rdfrules.data

import org.apache.jena.graph
import com.github.propi.rdfrules.utils.Stringifier

import scala.collection.TraversableView
import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem) {
  def toQuad: Quad = Quad(this)

  def toQuad(graph: TripleItem.Uri): Quad = Quad(this, graph)

  override def toString: String = Stringifier(this)
}

object Triple {

  type TripleTraversableView = TraversableView[Triple, Traversable[_]]

  implicit def tripleToJenaTriple(triple: Triple): graph.Triple = new graph.Triple(triple.subject, triple.predicate, triple.`object`)

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => v.subject + "  " + v.predicate + "  " + v.`object`

}