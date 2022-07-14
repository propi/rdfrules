package com.github.propi.rdfrules.rule

sealed trait InstantiatedAtom {
  def subject: Int

  def predicate: Int

  def `object`: Int

  def toAtom: Atom
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