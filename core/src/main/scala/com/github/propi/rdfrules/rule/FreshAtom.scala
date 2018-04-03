package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable) {
  def subjectPosition = TripleItemPosition.Subject(subject)

  def objectPosition = TripleItemPosition.Object(`object`)
}