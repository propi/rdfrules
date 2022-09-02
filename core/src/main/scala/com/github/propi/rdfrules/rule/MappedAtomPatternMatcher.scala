package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
trait MappedAtomPatternMatcher[T] extends PatternMatcher[T, AtomPattern.Mapped]

object MappedAtomPatternMatcher {

  def matchAtomItemPattern(item: Atom.Item, pattern: AtomItemPattern.Mapped)(implicit aliases: Aliases): Option[Aliases] = pattern match {
    case AtomItemPattern.Any => Some(aliases)
    case AtomItemPattern.AnyVariable => if (item.isInstanceOf[Atom.Variable]) Some(aliases) else None
    case AtomItemPattern.AnyConstant => if (item.isInstanceOf[Atom.Constant]) Some(aliases) else None
    case AtomItemPattern.Variable(x) => item match {
      case y: Atom.Variable =>
        aliases.get(y) match {
          case Some(y) if y.index == x.index => Some(aliases)
          case None => aliases + (y, x)
          case _ => None
        }
      case _ => None
    }
    case AtomItemPattern.Mapped.Constant(x) => item match {
      case Atom.Constant(y) if x.value == y => Some(aliases)
      case _ => None
    }
    case AtomItemPattern.Mapped.OneOf(x) => x.view.flatMap(matchAtomItemPattern(item, _)).headOption
    case AtomItemPattern.Mapped.NoneOf(x) => if (x.view.flatMap(matchAtomItemPattern(item, _)).isEmpty) Some(aliases) else None
  }

  def matchGraphPattern(atom: Atom.GraphAware, graphPattern: AtomItemPattern.Mapped)(implicit aliases: Aliases): Option[Aliases] = graphPattern match {
    case AtomItemPattern.Mapped.Constant(x) => if (atom.containsGraph(x.value)) Some(aliases) else None
    case AtomItemPattern.Mapped.OneOf(x) => x.view.flatMap(matchGraphPattern(atom, _)).headOption
    case AtomItemPattern.Mapped.NoneOf(x) => if (x.view.flatMap(matchGraphPattern(atom, _)).isEmpty) Some(aliases) else None
    case _ => Some(aliases)
  }

  def matchGraphPattern(atom: Atom, graphPattern: AtomItemPattern.Mapped)(implicit thi: TripleIndex.Builder[Int], aliases: Aliases): Option[Aliases] = graphPattern match {
    case _: AtomItemPattern.Mapped.Constant | _: AtomItemPattern.Mapped.OneOf | _: AtomItemPattern.Mapped.NoneOf => atom match {
      case atom: Atom.GraphAware => matchGraphPattern(atom, graphPattern)
      case _ => matchGraphPattern(atom.toGraphAwareAtom, graphPattern)
    }
    case _ => Some(aliases)
  }

  private def mayBeConstant(pattern: AtomItemPattern.Mapped): Boolean = pattern match {
    case AtomItemPattern.Any | AtomItemPattern.AnyConstant | AtomItemPattern.Mapped.Constant(_) | AtomItemPattern.Mapped.NoneOf(_) => true
    case AtomItemPattern.Mapped.OneOf(x) => x.exists(mayBeConstant)
    case _ => false
  }

  //TODO: support variables for predicates and graphs
  implicit def forAtom(implicit thi: TripleIndex.Builder[Int]): MappedAtomPatternMatcher[Atom] = new MappedAtomPatternMatcher[Atom] {
    def matchPattern(x: Atom, pattern: AtomPattern.Mapped)(implicit aliases: Aliases): Option[Aliases] = {
      matchAtomItemPattern(Atom.Constant(x.predicate), pattern.predicate).flatMap { implicit aliases =>
        matchAtomItemPattern(x.subject, pattern.subject)
      }.flatMap { implicit aliases =>
        matchAtomItemPattern(x.`object`, pattern.`object`)
      }.flatMap { implicit aliases =>
        matchGraphPattern(x, pattern.graph)
      }
    }
  }

  implicit val forFreshAtom: MappedAtomPatternMatcher[FreshAtom] = new MappedAtomPatternMatcher[FreshAtom] {
    def matchPattern(x: FreshAtom, pattern: AtomPattern.Mapped)(implicit aliases: Aliases): Option[Aliases] = {
      (if (mayBeConstant(pattern.subject)) {
        Some(aliases)
      } else {
        matchAtomItemPattern(x.subject, pattern.subject)
      }).flatMap { implicit aliases =>
        if (mayBeConstant(pattern.`object`)) {
          Some(aliases)
        } else {
          matchAtomItemPattern(x.`object`, pattern.`object`)
        }
      }
    }
  }

}