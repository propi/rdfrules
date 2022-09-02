package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.rule.PatternMatcher.Aliases

import java.util

trait PatternMatcher[T, P] {
  def matchPattern(x: T, pattern: P)(implicit aliases: Aliases): Option[Aliases]
}

object PatternMatcher {

  class Aliases private(private val hmap: Array[Int]) {
    def +(variable: Atom.Variable, patternVariable: Atom.Variable): Option[Aliases] = {
      val hasMultiMapping = hmap.iterator.zipWithIndex.filter(_._1 != 0).exists {
        case (value, key) => patternVariable.index == value - 1 && variable.index != key
      }
      if (hasMultiMapping) {
        None
      } else {
        val x = if (variable.index >= hmap.length) {
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
        Some(x)
      }

    }

    def contains(variable: Atom.Variable): Boolean = variable.index < hmap.length && hmap(variable.index) != 0

    def get(variable: Atom.Variable): Option[Atom.Variable] = if (contains(variable)) Some(Atom.Variable(hmap(variable.index) - 1)) else None

    override def equals(other: Any): Boolean = other match {
      case that: Aliases => util.Arrays.equals(hmap, that.hmap)
      case _ => false
    }

    override def hashCode(): Int = util.Arrays.hashCode(hmap)

    override def toString: String = hmap.iterator.zipWithIndex.filter(_._1 != 0).map(x => Atom.Variable(x._2) -> Atom.Variable(x._1 - 1)).mkString(", ")
  }

  object Aliases {
    lazy val empty: Aliases = new Aliases(Array.empty)
  }

}