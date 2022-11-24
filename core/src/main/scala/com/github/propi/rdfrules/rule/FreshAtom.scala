package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable) {
  lazy val variables: Set[Atom.Variable] = Set(subject, `object`)

  def subjectPosition: TripleItemPosition.Subject[Atom.Variable] = TripleItemPosition.Subject(subject)

  def objectPosition: TripleItemPosition.Object[Atom.Variable] = TripleItemPosition.Object(`object`)
}