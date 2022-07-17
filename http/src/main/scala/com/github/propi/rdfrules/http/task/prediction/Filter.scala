package com.github.propi.rdfrules.http.task.prediction

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition, TripleItemMatcher, TripleMatcher}
import com.github.propi.rdfrules.prediction.{PredictedResult, PredictedTriples}
import com.github.propi.rdfrules.rule.{Measure, RulePattern}
import com.github.propi.rdfrules.utils.TypedKeyMap

class Filter(predictedResults: Set[PredictedResult],
             distinctPrediction: Boolean,
             onlyFunctionalPredictions: Boolean,
             tripleMatchers: Seq[(TripleMatcher, Boolean)],
             measures: Seq[(Option[TypedKeyMap.Key[Measure]], TripleItemMatcher.Number)],
             patterns: Seq[RulePattern],
             indices: Set[Int]) extends Task[PredictedTriples, PredictedTriples] {
  val companion: TaskDefinition = Filter

  def execute(input: PredictedTriples): PredictedTriples = Function.chain[PredictedTriples](List(
    predictedTriples => if (indices.isEmpty) predictedTriples else predictedTriples.filterIndices(indices),
    predictedTriples => if (predictedResults.isEmpty) predictedTriples else predictedTriples.filter(x => predictedResults(x.predictedResult)),
    predictedTriples => if (tripleMatchers.isEmpty) predictedTriples else predictedTriples.filterResolved { predictedTriple =>
      tripleMatchers.exists { case (tripleMatcher, inverse) =>
        tripleMatcher.matchAll(predictedTriple.triple).matched ^ inverse
      }
    },
    predictedTriples => if (measures.nonEmpty) {
      predictedTriples.filter(predictedTriple => measures.forall { case (measure, matcher) =>
        measure match {
          case Some(measure) => predictedTriple.rule.measures.get(measure).collect {
            case Measure(x) => TripleItem.Number(x)
          }.exists(matcher.matchAll(_).nonEmpty)
          case None => matcher.matchAll(TripleItem.Number(predictedTriple.rule.ruleLength)).nonEmpty
        }
      })
    } else {
      predictedTriples
    },
    predictedTriples => patterns match {
      case Seq(head, tail@_*) => predictedTriples.filter(head, tail: _*)
      case _ => predictedTriples
    },
    predictedTriples => if (distinctPrediction) predictedTriples.distinctPredictions else predictedTriples,
    predictedTriples => if (onlyFunctionalPredictions) predictedTriples.onlyFunctionalPredictions else predictedTriples
  ))(input)
}

object Filter extends TaskDefinition {
  val name: String = "FilterPrediction"
}