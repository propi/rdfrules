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

    /*def apply(s: String)(implicit stringToTripleItem: String => Option[TripleItem]): Option[AtomItemPattern] = {
      val VariableChar = "\\?(\\w)".r
      s match {
        case VariableChar(char) => Some(Variable(Atom.Variable(char.head)))
        case x => stringToTripleItem(x).map(Constant)
      }
    }*/

  }

  /*def apply(subject: AtomItemPattern, predicate: Option[TripleItem.Uri], `object`: AtomItemPattern)
           (implicit mapper: TripleItem => Int): AtomPattern = {
    def aipToAi(x: AtomItemPattern): Atom.Item = x match {
      case AtomItemPattern.Variable(v) => v
      case AtomItemPattern.Constant(tripleItem) => Atom.Constant(mapper(tripleItem))
    }

    AtomPattern(aipToAi(subject), predicate.map(mapper), aipToAi(`object`))
  }*/

}