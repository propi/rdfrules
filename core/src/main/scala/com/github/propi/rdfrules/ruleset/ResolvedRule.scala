package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule
import com.github.propi.rdfrules.rule.{Measure, Rule}
import com.github.propi.rdfrules.ruleset.ResolvedRule.Atom
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
case class ResolvedRule private(body: IndexedSeq[Atom], head: Atom)(val measures: TypedKeyMap.Immutable[Measure]) {
  def ruleLength: Int = body.size + 1

  override def toString: String = Stringifier(this)
}

object ResolvedRule {

  sealed trait Atom {
    val subject: Atom.Item
    val predicate: TripleItem.Uri
    val `object`: Atom.Item

    override def equals(obj: scala.Any): Boolean = obj match {
      case x: Atom => (this eq x) || (subject == x.subject && predicate == x.predicate && `object` == x.`object`)
      case _ => false
    }
  }

  object Atom {

    sealed trait Item

    object Item {

      case class Variable private(value: String) extends Item

      case class Constant private(tripleItem: TripleItem) extends Item

      def apply(char: Char): Item = Variable("?" + char)

      def apply(variable: String): Item = Variable(variable)

      def apply(tripleItem: TripleItem): Item = Constant(tripleItem)

      implicit def apply(atomItem: rule.Atom.Item)(implicit mapper: TripleItemIndex): Item = atomItem match {
        case x: rule.Atom.Variable => apply(x.value)
        case rule.Atom.Constant(x) => apply(mapper.getTripleItem(x))
      }

    }

    case class Basic private(subject: Item, predicate: TripleItem.Uri, `object`: Item) extends Atom

    case class GraphBased private(subject: Item, predicate: TripleItem.Uri, `object`: Item)(val graphs: Set[TripleItem.Uri]) extends Atom

    def apply(subject: Item, predicate: TripleItem.Uri, `object`: Item): Atom = Basic(subject, predicate, `object`)

    implicit def apply(atom: rule.Atom)(implicit mapper: TripleItemIndex): Atom = atom match {
      case _: rule.Atom.Basic =>
        apply(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)
      case x: rule.Atom.GraphBased =>
        GraphBased(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)(x.graphsIterator.map(mapper.getTripleItem(_).asInstanceOf[TripleItem.Uri]).toSet)
    }

  }

  def simple(resolvedRule: ResolvedRule)(implicit mapper: TripleItemIndex): (Rule.Simple, collection.Map[Int, TripleItem]) = {
    var i = 0
    val map = collection.mutable.Map.empty[TripleItem, Int]

    @scala.annotation.tailrec
    def newIndex: Int = {
      i += 1
      mapper.getTripleItemOpt(i) match {
        case Some(_) => newIndex
        case None => i
      }
    }

    def tripleItem(x: TripleItem): Int = mapper.getIndexOpt(x) match {
      case Some(x) => x
      case None => map.getOrElseUpdate(x, newIndex)
    }

    def atomItem(x: Atom.Item): rule.Atom.Item = x match {
      case Atom.Item.Variable(x) => x
      case Atom.Item.Constant(x) => rule.Atom.Constant(tripleItem(x))
    }

    def atom(x: Atom): rule.Atom = rule.Atom(atomItem(x.subject), tripleItem(x.predicate), atomItem(x.`object`))

    Rule.Simple(
      atom(resolvedRule.head),
      resolvedRule.body.map(atom)
    )(resolvedRule.measures) -> map.map(_.swap)
  }

  def apply(body: IndexedSeq[Atom], head: Atom, measures: Measure*): ResolvedRule = ResolvedRule(body, head)(TypedKeyMap(measures))

  implicit def apply(rule: Rule.Simple)(implicit mapper: TripleItemIndex): ResolvedRule = ResolvedRule(
    rule.body.map(Atom.apply),
    rule.head
  )(rule.measures)

  implicit val itemStringifier: Stringifier[ResolvedRule.Atom.Item] = {
    case ResolvedRule.Atom.Item.Variable(x) => x
    case ResolvedRule.Atom.Item.Constant(x) => x.toString
  }

  implicit val atomStringifier: Stringifier[ResolvedRule.Atom] = {
    case ResolvedRule.Atom.Basic(s, p, o) => s"(${Stringifier(s)} ${p.toString} ${Stringifier(o)})"
    case v@ResolvedRule.Atom.GraphBased(s, p, o) =>
      def bracketGraphs(strGraphs: String): String = if (v.graphs.size == 1) strGraphs else s"[$strGraphs]"

      s"(${Stringifier(s)} ${p.toString} ${Stringifier(o)} ${bracketGraphs(v.graphs.iterator.map(_.toString).mkString(", "))})"
  }

  implicit val resolvedRuleStringifier: Stringifier[ResolvedRule] = (v: ResolvedRule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.iterator.toList.sortBy(_.companion).iterator.map(x => Stringifier(x)).mkString(", ")

  implicit val resolvedRuleOrdering: Ordering[ResolvedRule] = Ordering.by[ResolvedRule, TypedKeyMap.Immutable[Measure]](_.measures)

}