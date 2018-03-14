package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.rule.AtomPattern.AtomItemPattern

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: AtomItemPattern, predicate: AtomItemPattern, `object`: AtomItemPattern)

object AtomPattern {

  sealed trait AtomItemPattern

  object AtomItemPattern {

    case object Any extends AtomItemPattern

    case class OneOf(col: Traversable[AtomItemPattern]) extends AtomItemPattern

    case class NoneOf(col: Traversable[AtomItemPattern]) extends AtomItemPattern

    case object AnyVariable extends AtomItemPattern

    case object AnyConstant extends AtomItemPattern

    case class Variable(variable: Atom.Variable) extends AtomItemPattern

    case class Constant(constant: Atom.Constant) extends AtomItemPattern

    object Constant {
      def apply(constant: TripleItem)(implicit mapper: TripleItem => Int): Constant = Constant(Atom.Constant(mapper(constant)))
    }

  }

}