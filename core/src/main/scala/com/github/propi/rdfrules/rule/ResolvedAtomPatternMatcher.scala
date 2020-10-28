package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern
import com.github.propi.rdfrules.ruleset.ResolvedRule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait ResolvedAtomPatternMatcher[T] extends PatternMatcher[T, AtomPattern] {
  def matchPattern(x: T, pattern: AtomPattern): Boolean
}

object ResolvedAtomPatternMatcher {

  def matchAtomItemPattern(item: ResolvedRule.Atom.Item, pattern: AtomItemPattern): Boolean = pattern match {
    case AtomItemPattern.Any => true
    case AtomItemPattern.AnyVariable => item.isInstanceOf[ResolvedRule.Atom.Item.Variable]
    case AtomItemPattern.AnyConstant => item.isInstanceOf[ResolvedRule.Atom.Item.Constant]
    case AtomItemPattern.Variable(x) => item match {
      case ResolvedRule.Atom.Item.Variable(y) => x.value == y
      case _ => false
    }
    case AtomItemPattern.Constant(x) => item match {
      case ResolvedRule.Atom.Item.Constant(y) => x == y
      case _ => false
    }
    case AtomItemPattern.OneOf(x) => x.exists(matchAtomItemPattern(item, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchAtomItemPattern(item, _))
  }

  def matchGraphPattern(atom: ResolvedRule.Atom.GraphBased, graphPattern: AtomItemPattern): Boolean = graphPattern match {
    case AtomItemPattern.Constant(x: TripleItem.Uri) => atom.graphs(x)
    case AtomItemPattern.OneOf(x) => x.exists(matchGraphPattern(atom, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchGraphPattern(atom, _))
    case _ => true
  }

  def matchGraphPattern(atom: ResolvedRule.Atom, graphPattern: AtomItemPattern): Boolean = graphPattern match {
    case _: AtomItemPattern.Constant | _: AtomItemPattern.OneOf | _: AtomItemPattern.NoneOf => atom match {
      case _: ResolvedRule.Atom.Basic => true
      case atom: ResolvedRule.Atom.GraphBased => matchGraphPattern(atom, graphPattern)
    }
    case _ => true
  }

  implicit val forResolvedAtom: ResolvedAtomPatternMatcher[ResolvedRule.Atom] = (x: ResolvedRule.Atom, pattern: AtomPattern) => matchAtomItemPattern(x.subject, pattern.subject) &&
    matchAtomItemPattern(ResolvedRule.Atom.Item.Constant(x.predicate), pattern.predicate) &&
    matchAtomItemPattern(x.`object`, pattern.`object`) &&
    matchGraphPattern(x, pattern.graph)

}