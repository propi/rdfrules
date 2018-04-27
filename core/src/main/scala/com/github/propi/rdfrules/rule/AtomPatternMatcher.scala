package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern

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

  def matchGraphPattern(atom: Atom, graphPattern: AtomItemPattern)(implicit thi: TripleHashIndex): Boolean = graphPattern match {
    case AtomItemPattern.Constant(x) => (atom.subject, atom.`object`) match {
      case (_: Atom.Variable, _: Atom.Variable) => thi.isInGraph(x.value, atom.predicate)
      case (_: Atom.Constant, _: Atom.Variable) => thi.isInGraph(x.value, atom.predicate, atom.subjectPosition)
      case (_: Atom.Variable, _: Atom.Constant) => thi.isInGraph(x.value, atom.predicate, atom.objectPosition)
      case (Atom.Constant(s), Atom.Constant(o)) => thi.isInGraph(x.value, s, atom.predicate, o)
    }
    case AtomItemPattern.OneOf(x) => x.exists(matchGraphPattern(atom, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchGraphPattern(atom, _))
    case _ => true
  }

  private def mayBeConstant(pattern: AtomItemPattern): Boolean = pattern match {
    case AtomItemPattern.Any | AtomItemPattern.AnyConstant | AtomItemPattern.Constant(_) | AtomItemPattern.NoneOf(_) => true
    case AtomItemPattern.OneOf(x) => x.exists(mayBeConstant)
    case _ => false
  }

  //TODO: support variables for predicates and graphs
  implicit def forAtom(implicit thi: TripleHashIndex): AtomPatternMatcher[Atom] = (x: Atom, pattern: AtomPattern) => matchAtomItemPattern(x.subject, pattern.subject) &&
    matchAtomItemPattern(Atom.Constant(x.predicate), pattern.predicate) &&
    matchAtomItemPattern(x.`object`, pattern.`object`) &&
    matchGraphPattern(x, pattern.graph)

  implicit val forFreshAtom: AtomPatternMatcher[FreshAtom] = (x: FreshAtom, pattern: AtomPattern) => (mayBeConstant(pattern.subject) || matchAtomItemPattern(x.subject, pattern.subject)) &&
    (mayBeConstant(pattern.`object`) || matchAtomItemPattern(x.`object`, pattern.`object`))

}