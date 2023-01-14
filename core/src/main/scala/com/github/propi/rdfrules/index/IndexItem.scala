package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.rule.Atom

/**
  * Created by Vaclav Zeman on 3. 9. 2020.
  */
sealed trait IndexItem[T]

object IndexItem {

  type IntQuad = Quad[Int]
  type IntTriple = Triple[Int]

  case class Quad[T](s: T, p: T, o: T, g: T) extends IndexItem[T]

  case class Triple[T](s: T, p: T, o: T) extends IndexItem[T] {
    final def target(position: TriplePosition): T = position match {
      case TriplePosition.Subject => s
      case TriplePosition.Object => o
      case TriplePosition.Predicate => p
    }
  }

  case class SameAs[T](s: T, o: T) extends IndexItem[T]

  implicit class PimpedIntQuad(x: IntQuad)(implicit mapper: TripleItemIndex) {
    def toTriple: data.Triple = Triple(x.s, x.p, x.o).toTriple

    def toQuad: data.Quad = data.Quad(toTriple, mapper.getTripleItem(x.g).asInstanceOf[TripleItem.Uri])
  }

  implicit class PimpedIntTriple(x: IntTriple)(implicit mapper: TripleItemIndex) {
    def toTriple: data.Triple = data.Triple(
      mapper.getTripleItem(x.s).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.p).asInstanceOf[TripleItem.Uri],
      mapper.getTripleItem(x.o)
    )
  }

  def apply(atom: Atom): Option[IntTriple] = (atom.subject, atom.`object`) match {
    case (Atom.Constant(s), Atom.Constant(o)) => Some(Triple(s, atom.predicate, o))
    case _ => None
  }

}