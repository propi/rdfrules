package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.rule.PatternMatcher.Aliases

trait PatternMatcher[T, P] {
  def matchPattern(x: T, pattern: P): Option[Aliases]
}

object PatternMatcher {

  class Aliases private(hmap: Array[Int]) {
    def +(variable: Atom.Variable, patternVariable: Atom.Variable): Aliases = {
      if (variable.index >= hmap.length) {
        val newArray = new Array[Int](variable.index + 1)
        Array.copy(hmap, 0, newArray, 0, hmap.length)
        newArray(variable.index) = patternVariable.index + 1
        new Aliases(newArray)
      } else if (hmap(variable.index) - 1 == patternVariable.index) {
        this
      } else {
        val newArray = hmap.clone()
        newArray(variable.index) = patternVariable.index + 1
        new Aliases(newArray)
      }
    }

    def contains(variable: Atom.Variable): Boolean = variable.index < hmap.length && hmap(variable.index) != 0

    def get(variable: Atom.Variable): Option[Atom.Variable] = if (contains(variable)) Some(Atom.Variable(hmap(variable.index) - 1)) else None
  }

  object Aliases {
    def empty: Aliases = new Aliases(Array.empty)
  }

}