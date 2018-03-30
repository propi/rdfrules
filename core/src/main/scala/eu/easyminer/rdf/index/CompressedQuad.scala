package eu.easyminer.rdf.index

import eu.easyminer.rdf.data
import eu.easyminer.rdf.data.{Quad, TripleItem}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
case class CompressedQuad(subject: Int, predicate: Int, `object`: Int, graph: Int)

object CompressedQuad {

  implicit class PimpedCompressedQuad(x: CompressedQuad)(implicit mapper: TripleItemHashIndex) {
    def toTriple: data.Triple = data.Triple(
      mapper.getTripleItem(x.subject).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.predicate).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.`object`)
    )

    def toQuad: data.Quad = data.Quad(toTriple, mapper.getTripleItem(x.graph).asInstanceOf[TripleItem.Uri])
  }

}
