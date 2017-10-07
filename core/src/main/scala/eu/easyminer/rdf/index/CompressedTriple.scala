package eu.easyminer.rdf.index

import eu.easyminer.rdf.data.{Triple, TripleItem}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class CompressedTriple(subject: Int, predicate: Int, `object`: Int)

object CompressedTriple {

  implicit def tripleToCompressedTriple(triple: Triple)(implicit map: collection.Map[TripleItem, Int]): CompressedTriple = CompressedTriple(map(triple.subject), map(triple.predicate), map(triple.`object`))

}