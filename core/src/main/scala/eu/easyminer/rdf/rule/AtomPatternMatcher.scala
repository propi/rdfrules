package eu.easyminer.rdf.rule

import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.AtomPattern.AtomItemPattern

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait AtomPatternMatcher[T] {
  def matchPattern(x: T, pattern: AtomPattern): Boolean
}

object AtomPatternMatcher {

  def matchAtomItemPattern(item: Atom.Item, pattern: AtomItemPattern): Boolean = pattern match {
    case AtomItemPattern.Any => true
    case AtomItemPattern.AnyVariable => item.isInstanceOf[Atom.Variable]
    case AtomItemPattern.AnyConstant => item.isInstanceOf[Atom.Constant]
    case AtomItemPattern.Variable(x) => item match {
      case Atom.Variable(y) => x.index == y
      case _ => false
    }
    case AtomItemPattern.Constant(x) => item match {
      case Atom.Constant(y) => x.value == y
      case _ => false
    }
    case AtomItemPattern.OneOf(x) => x.exists(matchAtomItemPattern(item, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchAtomItemPattern(item, _))
  }

  private def mayBeConstant(pattern: AtomItemPattern): Boolean = pattern match {
    case AtomItemPattern.Any | AtomItemPattern.AnyConstant | AtomItemPattern.Constant(_) | AtomItemPattern.NoneOf(_) => true
    case AtomItemPattern.OneOf(x) => x.exists(mayBeConstant)
    case _ => false
  }

  class ForAtom(implicit thi: TripleHashIndex) extends AtomPatternMatcher[Atom] {
    def matchPattern(x: Atom, pattern: AtomPattern): Boolean = matchAtomItemPattern(x.subject, pattern.subject) &&
      matchAtomItemPattern(Atom.Constant(x.predicate), pattern.predicate) &&
      matchAtomItemPattern(x.`object`, pattern.`object`) &&
      x.graphs.exists(x => matchAtomItemPattern(Atom.Constant(x), pattern.graph))
  }

  object ForFreshAtom extends AtomPatternMatcher[FreshAtom] {
    def matchPattern(x: FreshAtom, pattern: AtomPattern): Boolean = (mayBeConstant(pattern.subject) || matchAtomItemPattern(x.subject, pattern.subject)) &&
      (mayBeConstant(pattern.`object`) || matchAtomItemPattern(x.`object`, pattern.`object`))
  }

}