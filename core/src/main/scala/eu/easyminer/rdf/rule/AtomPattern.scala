package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.index.TripleItemHashIndex
import eu.easyminer.rdf.rule.AtomPattern.AtomItemPattern
import eu.easyminer.rdf.rule.AtomPattern.AtomItemPattern.Any

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: AtomItemPattern = Any, predicate: AtomItemPattern = Any, `object`: AtomItemPattern = Any, graph: AtomItemPattern = Any)

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
      def apply(constant: TripleItem)(implicit mapper: TripleItemHashIndex): Constant = Constant(Atom.Constant(mapper.getIndex(constant)))
    }

  }

}