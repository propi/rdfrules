package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.IndexItem
import com.github.propi.rdfrules.index.IndexItem.IntTriple

sealed trait InstantiatedAtom {
  def subject: Int

  def predicate: Int

  def `object`: Int

  def toAtom: Atom

  final def toTriple: IntTriple = IndexItem.Triple(subject, predicate, `object`)
}

object InstantiatedAtom {

  private case class Basic(subject: Int, predicate: Int, `object`: Int) extends InstantiatedAtom {
    def toAtom: Atom = Atom(Atom.Constant(subject), predicate, Atom.Constant(`object`))
  }

  def apply(subject: Int, predicate: Int, `object`: Int): InstantiatedAtom = Basic(subject, predicate, `object`)

  implicit class PimpedAtom(val atom: Atom) extends AnyVal {
    def toInstantiatedAtom: Option[InstantiatedAtom] = (atom.subject, atom.`object`) match {
      case (Atom.Constant(s), Atom.Constant(o)) => Some(apply(s, atom.predicate, o))
      case _ => None
    }
  }

}