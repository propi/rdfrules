package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.{Dataset, Graph, Triple}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
class PredictionResult private(_predictedTriples: Traversable[PredictedTriple], val index: Index, _distinct: Boolean = false) {

  def distinct: PredictionResult = new PredictionResult(_predictedTriples.distinct, index, true)

  def onlyFunctionalProperties: PredictionResult = new PredictionResult(_predictedTriples.distinctBy(x => x.triple.subject -> x.triple.predicate), index, true)

  def predictedTriples: TraversableView[PredictedTriple, Traversable[PredictedTriple]] = _predictedTriples.view

  def triples: Traversable[Triple] = predictedTriples.map(_.triple)

  def graph: Graph = if (_distinct) {
    Graph(triples, false)
  } else {
    Graph(distinct.triples, false)
  }

  def mergedDataset: Dataset = index.toDataset + graph

  def evaluate(withModel: Boolean = false): EvaluationResult = {
    index.tripleItemMap { mapper =>
      index.tripleMap { thi =>
        val headPredicates = collection.mutable.Set.empty[Int]
        val modelSet = collection.mutable.LinkedHashSet.empty[ResolvedRule]
        val evaluationResult = (if (_distinct) predictedTriples else distinct.predictedTriples).foldLeft(EvaluationResult(0, 0, 0, Vector.empty)) { (result, predictedTriple) =>
          val p = mapper.getIndexOpt(predictedTriple.triple.predicate)
          val isTrue = predictedTriple.existing
          val fn = p match {
            case Some(p) if !headPredicates(p) =>
              headPredicates += p
              result.fn + thi.predicates.get(p).map(_.size).getOrElse(0)
            case _ => result.fn
          }
          if (isTrue && withModel) modelSet += predictedTriple.rule
          EvaluationResult(
            if (isTrue) result.tp + 1 else result.tp,
            if (isTrue) result.fp else result.fp + 1,
            if (isTrue) fn - 1 else fn,
            result.model
          )
        }
        if (withModel) evaluationResult.copy(model = modelSet.toVector) else evaluationResult
      }
    }
  }

}

object PredictionResult {

  def apply(triples: Traversable[PredictedTriple], index: Index): PredictionResult = new PredictionResult(triples, index)

}