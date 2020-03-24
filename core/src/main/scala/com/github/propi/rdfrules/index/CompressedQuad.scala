package com.github.propi.rdfrules.index

import com.github.propi.rdfrules
import com.github.propi.rdfrules.data.{Quad, Triple, TripleItem}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
case class CompressedQuad(subject: Int, predicate: Int, `object`: Int, graph: Int) {
  def toIndexedQuad: TripleHashIndex.Quad[Int] = new TripleHashIndex.Quad[Int](subject, predicate, `object`, graph)
}

object CompressedQuad {

  implicit class PimpedCompressedQuad(x: CompressedQuad)(implicit mapper: TripleItemHashIndex) {
    def toTriple: Triple = rdfrules.data.Triple(
      mapper.getTripleItem(x.subject).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.predicate).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.`object`)
    )

    def toQuad: Quad = rdfrules.data.Quad(toTriple, mapper.getTripleItem(x.graph).asInstanceOf[TripleItem.Uri])
  }

}
