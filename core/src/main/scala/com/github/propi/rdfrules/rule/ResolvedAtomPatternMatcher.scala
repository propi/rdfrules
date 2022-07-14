package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait ResolvedAtomPatternMatcher[T] extends PatternMatcher[T, AtomPattern] {
  def matchPattern(x: T, pattern: AtomPattern): Boolean
}

object ResolvedAtomPatternMatcher {

  def matchAtomItemPattern(item: ResolvedItem, pattern: AtomItemPattern): Boolean = pattern match {
    case AtomItemPattern.Any => true
    case AtomItemPattern.AnyVariable => item.isInstanceOf[ResolvedItem.Variable]
    case AtomItemPattern.AnyConstant => item.isInstanceOf[ResolvedItem.Constant]
    case AtomItemPattern.Variable(x) => item match {
      case ResolvedItem.Variable(y) => x.value == y
      case _ => false
    }
    case AtomItemPattern.Constant(x) => item match {
      case ResolvedItem.Constant(y) => x == y
      case _ => false
    }
    case AtomItemPattern.OneOf(x) => x.exists(matchAtomItemPattern(item, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchAtomItemPattern(item, _))
  }

  def matchGraphPattern(atom: ResolvedAtom.GraphAware, graphPattern: AtomItemPattern): Boolean = graphPattern match {
    case AtomItemPattern.Constant(x: TripleItem.Uri) => atom.graphs(x)
    case AtomItemPattern.OneOf(x) => x.exists(matchGraphPattern(atom, _))
    case AtomItemPattern.NoneOf(x) => !x.exists(matchGraphPattern(atom, _))
    case _ => true
  }

  def matchGraphPattern(atom: ResolvedAtom, graphPattern: AtomItemPattern): Boolean = graphPattern match {
    case _: AtomItemPattern.Constant | _: AtomItemPattern.OneOf | _: AtomItemPattern.NoneOf => atom match {
      case atom: ResolvedAtom.GraphAware => matchGraphPattern(atom, graphPattern)
      case _ => true
    }
    case _ => true
  }

  implicit val forResolvedAtom: ResolvedAtomPatternMatcher[ResolvedAtom] = (x: ResolvedAtom, pattern: AtomPattern) => matchAtomItemPattern(x.subject, pattern.subject) &&
    matchAtomItemPattern(ResolvedItem.Constant(x.predicate), pattern.predicate) &&
    matchAtomItemPattern(x.`object`, pattern.`object`) &&
    matchGraphPattern(x, pattern.graph)

}