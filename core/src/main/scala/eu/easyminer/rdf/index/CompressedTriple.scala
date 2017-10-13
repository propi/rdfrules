package eu.easyminer.rdf.index

import eu.easyminer.rdf.data.{Triple, TripleItem}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class CompressedTriple(subject: Int, predicate: Int, `object`: Int)

object CompressedTriple {

  def apply(triple: Triple)(implicit mapper: TripleItem => Int): CompressedTriple = CompressedTriple(mapper(triple.subject), mapper(triple.predicate), mapper(triple.`object`))

  def toTriple(triple: CompressedTriple)(implicit mapper: Int => TripleItem): Triple = Triple(mapper(triple.subject).asInstanceOf[TripleItem.Uri], mapper(triple.predicate).asInstanceOf[TripleItem.Uri], mapper(triple.`object`))

}