package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition, TripleItemMatcher}
import com.github.propi.rdfrules.rule.{Measure, RulePattern}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class FilterRules(measures: Seq[(Option[TypedKeyMap.Key[Measure]], TripleItemMatcher.Number)],
                  patterns: Seq[RulePattern],
                  indices: Set[Int]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = FilterRules

  def execute(input: Ruleset): Ruleset = Function.chain[Ruleset](List(
    ruleset => if (indices.isEmpty) ruleset else ruleset.filterIndices(indices),
    ruleset => if (measures.nonEmpty) {
      ruleset.filter(rule => measures.forall { case (measure, matcher) =>
        measure match {
          case Some(measure) => rule.measures.get(measure).collect {
            case Measure(x) => TripleItem.Number(x)
          }.exists(matcher.matchAll(_).nonEmpty)
          case None => matcher.matchAll(TripleItem.Number(rule.ruleLength)).nonEmpty
        }
      })
    } else {
      ruleset
    },
    ruleset => patterns match {
      case Seq(head, tail@_*) => ruleset.filter(head, tail: _*)
      case _ => ruleset
    }
  ))(input)
}

object FilterRules extends TaskDefinition {
  val name: String = "FilterRules"
}