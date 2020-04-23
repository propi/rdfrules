package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.rule.Atom

/**
  * Created by Vaclav Zeman on 23. 4. 2020.
  */
case class CompressedTriple(subject: Int, predicate: Int, `object`: Int) {
  def toCompressedQuad(graph: Int): CompressedQuad = CompressedQuad(subject, predicate, `object`, graph)
}

object CompressedTriple {

  def apply(atom: Atom): Option[CompressedTriple] = (atom.subject, atom.`object`) match {
    case (Atom.Constant(s), Atom.Constant(o)) => Some(CompressedTriple(s, atom.predicate, o))
    case _ => None
  }

  implicit class PimpedCompressedTriple(x: CompressedTriple)(implicit mapper: TripleItemHashIndex) {
    def toTriple: Triple = x.toCompressedQuad(0).toTriple
  }

}