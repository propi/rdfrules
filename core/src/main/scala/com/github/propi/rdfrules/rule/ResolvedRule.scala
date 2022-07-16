package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
case class ResolvedRule private(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom)(val measures: TypedKeyMap.Immutable[Measure]) {
  def ruleLength: Int = body.size + 1

  def toRule(implicit tripleItemIndex: TripleItemIndex): FinalRule = Rule(
    head.toAtom,
    body.map(_.toAtom)
  )(measures)

  override def toString: String = Stringifier(this)
}

object ResolvedRule {

  /*def simple(resolvedRule: ResolvedRule)(implicit mapper: TripleItemIndex): (FinalRule, collection.Map[Int, TripleItem]) = {
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

    def atomItem(x: ResolvedItem): rule.Atom.Item = x match {
      case ResolvedItem.Variable(x) => x
      case ResolvedItem.Constant(x) => rule.Atom.Constant(tripleItem(x))
    }

    def atom(x: ResolvedAtom): rule.Atom = rule.Atom(atomItem(x.subject), tripleItem(x.predicate), atomItem(x.`object`))

    Rule(
      atom(resolvedRule.head),
      resolvedRule.body.map(atom)
    )(resolvedRule.measures) -> map.map(_.swap)
  }*/

  def apply(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom, measures: Measure*): ResolvedRule = ResolvedRule(body, head)(TypedKeyMap(measures))

  implicit def apply(rule: FinalRule)(implicit mapper: TripleItemIndex): ResolvedRule = ResolvedRule(
    rule.body.map(ResolvedAtom.apply),
    rule.head
  )(rule.measures)

  implicit val itemStringifier: Stringifier[ResolvedItem] = {
    case ResolvedItem.Variable(x) => x
    case ResolvedItem.Constant(x) => x.toString
  }

  implicit val atomStringifier: Stringifier[ResolvedAtom] = {
    case x: ResolvedAtom.GraphAware =>
      def bracketGraphs(strGraphs: String): String = if (x.graphs.size == 1) strGraphs else s"[$strGraphs]"

      s"(${Stringifier(x.subject)} ${x.predicate.toString} ${Stringifier(x.`object`)} ${bracketGraphs(x.graphs.iterator.map(_.toString).mkString(", "))})"
    case x => s"(${Stringifier(x.subject)} ${x.predicate.toString} ${Stringifier(x.`object`)})"
  }

  implicit val resolvedRuleStringifier: Stringifier[ResolvedRule] = (v: ResolvedRule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.iterator.toList.sortBy(_.companion).iterator.map(x => Stringifier(x)).mkString(", ")

  implicit val resolvedRuleOrdering: Ordering[ResolvedRule] = Ordering.by[ResolvedRule, TypedKeyMap.Immutable[Measure]](_.measures)

}