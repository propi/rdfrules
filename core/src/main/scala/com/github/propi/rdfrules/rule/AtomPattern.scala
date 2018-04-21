package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemHashIndex
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern.Any

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: AtomItemPattern = Any, predicate: AtomItemPattern = Any, `object`: AtomItemPattern = Any, graph: AtomItemPattern = Any)

object AtomPattern {

  sealed trait AtomItemPattern

  object AtomItemPattern {

    case object Any extends AtomItemPattern

    case class OneOf(col: Traversable[AtomItemPattern]) extends AtomItemPattern

    object OneOf {
      def apply(atomItemPattern: AtomItemPattern, atomItemPatterns: AtomItemPattern*): OneOf = new OneOf(atomItemPattern +: atomItemPatterns)
    }

    case class NoneOf(col: Traversable[AtomItemPattern]) extends AtomItemPattern

    object NoneOf {
      def apply(atomItemPattern: AtomItemPattern, atomItemPatterns: AtomItemPattern*): NoneOf = new NoneOf(atomItemPattern +: atomItemPatterns)
    }

    case object AnyVariable extends AtomItemPattern

    case object AnyConstant extends AtomItemPattern

    case class Variable(variable: Atom.Variable) extends AtomItemPattern

    case class Constant(constant: Atom.Constant) extends AtomItemPattern

    object Constant {
      def apply(constant: TripleItem)(implicit mapper: TripleItemHashIndex): Constant = Constant(Atom.Constant(mapper.getIndex(constant)))
    }

    implicit def apply(tripleItem: TripleItem)(implicit mapper: TripleItemHashIndex): AtomItemPattern = Constant(tripleItem)

    implicit def apply(variable: Char): AtomItemPattern = Variable(variable)

  }

}