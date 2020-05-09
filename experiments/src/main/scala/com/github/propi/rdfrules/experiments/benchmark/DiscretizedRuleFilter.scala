package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.{Index, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.{Atom, Rule, RuleConstraint}
import com.github.propi.rdfrules.utils.TypedKeyMap.Key

/**
  * Created by Vaclav Zeman on 15. 4. 2020.
  */
case class DiscretizedRuleFilter(discretizedPredicates: Seq[TripleItem.Uri]) extends RuleConstraint.Filter {

  def mapped(implicit mapper: TripleItemHashIndex): RuleConstraint.MappedFilter = {
    val discretizedGroups = discretizedPredicates.iterator.collect {
      case x@TripleItem.LongUri(uri) => x -> uri
      case x: TripleItem.PrefixedUri => x -> x.toLongUri.uri
    }.map(x => x._1 -> x._2.replaceFirst("_discretized_level_.*", ""))
      .toSeq
      .groupBy(_._2)
      .mapValues(_.iterator.map(_._1).map(mapper.getIndex(_)).toSet)
      .valuesIterator
      .flatMap(x => x.iterator.map(_ -> x))
      .toMap
    (newAtom: Atom, rule: Option[Rule]) =>
      rule match {
        case Some(rule) => discretizedGroups.get(newAtom.predicate).forall(pSet => (Iterator(rule.head) ++ rule.body.iterator).forall(x => newAtom.predicate == x.predicate || !pSet(x.predicate)))
        case None => true
      }
  }

  def companion: DiscretizedRuleFilter.type = DiscretizedRuleFilter

}

object DiscretizedRuleFilter extends Key[DiscretizedRuleFilter] {

  def apply(index: Index): DiscretizedRuleFilter = {
    val discretizedPredicates = index.tripleMap { thi =>
      index.tripleItemMap { mapper =>
        thi.predicates.keysIterator.map(mapper.getTripleItem).collect {
          case x@TripleItem.LongUri(uri) if uri.contains("_discretized_level_") => x
          case x: TripleItem.PrefixedUri if x.toLongUri.uri.contains("_discretized_level_") => x
        }.toList
      }
    }
    new DiscretizedRuleFilter(discretizedPredicates)
  }

}