package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{IndexItem, TripleItemIndex}
import com.github.propi.rdfrules.utils.{ForEach, Stringifier}
import org.apache.jena.graph

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple private(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem) {
  def toQuad: Quad = Quad(this)

  def toQuad(graph: TripleItem.Uri): Quad = Quad(this, graph)

  def intern: Triple = new Triple(subject.intern, predicate.intern, `object`.intern)

  def copy(subject: TripleItem.Uri = subject, predicate: TripleItem.Uri = predicate, `object`: TripleItem = `object`): Triple = Triple(subject, predicate, `object`)

  def toIndexedTriple(implicit tripleItemIndex: TripleItemIndex): IntTriple = IndexItem.Triple(tripleItemIndex.getIndex(subject), tripleItemIndex.getIndex(predicate), tripleItemIndex.getIndex(`object`))

  override def toString: String = Stringifier(this)
}

object Triple {

  def apply(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem): Triple = new Triple(subject, predicate, `object`)

  type TripleTraversableView = ForEach[Triple]

  implicit def tripleToJenaTriple(triple: Triple): graph.Triple = new graph.Triple(triple.subject, triple.predicate, triple.`object`)

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => s"${v.subject}  ${v.predicate}  ${v.`object`}"

}