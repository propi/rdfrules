package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait MappedAtomPatternMatcher[T] extends PatternMatcher[T, AtomPattern.Mapped]

object MappedAtomPatternMatcher {

  def matchAtomItemPattern(item: Atom.Item, pattern: AtomItemPattern.Mapped): Boolean = pattern match {
    case AtomItemPattern.Any => true
    case AtomItemPattern.AnyVariable => item.isInstanceOf[Atom.Variable]
    case AtomItemPattern.AnyConstant => item.isInstanceOf[Atom.Constant]
    case AtomItemPattern.Variable(x) => item match {
      case Atom.Variable(y) => x.index == y
      case _ => false
    }
    case AtomItemPattern.Mapped.Constant(x) => item match {
      case Atom.Constant(y) => x.value == y
      case _ => false
    }
    case AtomItemPattern.Mapped.OneOf(x) => x.exists(matchAtomItemPattern(item, _))
    case AtomItemPattern.Mapped.NoneOf(x) => !x.exists(matchAtomItemPattern(item, _))
  }

  def matchGraphPattern(atom: Atom.GraphAware, graphPattern: AtomItemPattern.Mapped): Boolean = graphPattern match {
    case AtomItemPattern.Mapped.Constant(x) => atom.containsGraph(x.value)
    case AtomItemPattern.Mapped.OneOf(x) => x.exists(matchGraphPattern(atom, _))
    case AtomItemPattern.Mapped.NoneOf(x) => !x.exists(matchGraphPattern(atom, _))
    case _ => true
  }

  def matchGraphPattern(atom: Atom, graphPattern: AtomItemPattern.Mapped)(implicit thi: TripleIndex.Builder[Int]): Boolean = graphPattern match {
    case _: AtomItemPattern.Mapped.Constant | _: AtomItemPattern.Mapped.OneOf | _: AtomItemPattern.Mapped.NoneOf => atom match {
      case atom: Atom.GraphAware => matchGraphPattern(atom, graphPattern)
      case _ => matchGraphPattern(atom.toGraphAwareAtom, graphPattern)
    }
    case _ => true
  }

  private def mayBeConstant(pattern: AtomItemPattern.Mapped): Boolean = pattern match {
    case AtomItemPattern.Any | AtomItemPattern.AnyConstant | AtomItemPattern.Mapped.Constant(_) | AtomItemPattern.Mapped.NoneOf(_) => true
    case AtomItemPattern.Mapped.OneOf(x) => x.exists(mayBeConstant)
    case _ => false
  }

  //TODO: support variables for predicates and graphs
  implicit def forAtom(implicit thi: TripleIndex.Builder[Int]): MappedAtomPatternMatcher[Atom] = (x: Atom, pattern: AtomPattern.Mapped) => {
    matchAtomItemPattern(Atom.Constant(x.predicate), pattern.predicate) &&
      matchAtomItemPattern(x.subject, pattern.subject) &&
      matchAtomItemPattern(x.`object`, pattern.`object`) &&
      matchGraphPattern(x, pattern.graph)
  }

  implicit val forFreshAtom: MappedAtomPatternMatcher[FreshAtom] = (x: FreshAtom, pattern: AtomPattern.Mapped) => {
    (mayBeConstant(pattern.subject) || matchAtomItemPattern(x.subject, pattern.subject)) &&
      (mayBeConstant(pattern.`object`) || matchAtomItemPattern(x.`object`, pattern.`object`))
  }

}