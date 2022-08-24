package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait ResolvedAtomPatternMatcher[T] extends PatternMatcher[T, AtomPattern]

object ResolvedAtomPatternMatcher {

  def matchAtomItemPattern(item: ResolvedItem, pattern: AtomItemPattern)(implicit aliases: Aliases): Option[Aliases] = pattern match {
    case AtomItemPattern.Any => Some(aliases)
    case AtomItemPattern.AnyVariable => if (item.isInstanceOf[ResolvedItem.Variable]) Some(aliases) else None
    case AtomItemPattern.AnyConstant => if (item.isInstanceOf[ResolvedItem.Constant]) Some(aliases) else None
    case AtomItemPattern.Variable(x) => item match {
      case y: ResolvedItem.Variable =>
        val yVar = y.toVariable
        aliases.get(yVar) match {
          case Some(y) if y.index == x.index => Some(aliases)
          case None => Some(aliases + (yVar, x))
          case _ => None
        }
      case _ => None
    }
    case AtomItemPattern.Constant(x) => item match {
      case ResolvedItem.Constant(y) if x == y => Some(aliases)
      case _ => None
    }
    case AtomItemPattern.OneOf(x) => x.view.flatMap(matchAtomItemPattern(item, _)).headOption
    case AtomItemPattern.NoneOf(x) => if (x.view.flatMap(matchAtomItemPattern(item, _)).isEmpty) Some(aliases) else None
  }

  def matchGraphPattern(atom: ResolvedAtom.GraphAware, graphPattern: AtomItemPattern)(implicit aliases: Aliases): Option[Aliases] = graphPattern match {
    case AtomItemPattern.Constant(x: TripleItem.Uri) => if (atom.graphs(x)) Some(aliases) else None
    case AtomItemPattern.OneOf(x) => x.view.flatMap(matchGraphPattern(atom, _)).headOption
    case AtomItemPattern.NoneOf(x) => if (x.view.flatMap(matchGraphPattern(atom, _)).isEmpty) Some(aliases) else None
    case _ => Some(aliases)
  }

  def matchGraphPattern(atom: ResolvedAtom, graphPattern: AtomItemPattern)(implicit aliases: Aliases): Option[Aliases] = graphPattern match {
    case _: AtomItemPattern.Constant | _: AtomItemPattern.OneOf | _: AtomItemPattern.NoneOf => atom match {
      case atom: ResolvedAtom.GraphAware => matchGraphPattern(atom, graphPattern)
      case _ => Some(aliases)
    }
    case _ => Some(aliases)
  }

  implicit val forResolvedAtom: ResolvedAtomPatternMatcher[ResolvedAtom] = new ResolvedAtomPatternMatcher[ResolvedAtom] {
    def matchPattern(x: ResolvedAtom, pattern: AtomPattern)(implicit aliases: Aliases): Option[Aliases] = {
      matchAtomItemPattern(x.subject, pattern.subject).flatMap { implicit aliases =>
        matchAtomItemPattern(ResolvedItem.Constant(x.predicate), pattern.predicate)
      }.flatMap { implicit aliases =>
        matchAtomItemPattern(x.`object`, pattern.`object`)
      }.flatMap { implicit aliases =>
        matchGraphPattern(x, pattern.graph)
      }
    }
  }

}