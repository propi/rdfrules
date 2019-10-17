package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.{Dataset, Graph, Triple}
import com.github.propi.rdfrules.index.Index
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
    Graph(triples)
  } else {
    Graph(distinct.triples)
  }

  def mergedDataset: Dataset = index.toDataset + graph

  def evaluate: EvaluationResult = {
    index.tripleItemMap { mapper =>
      index.tripleMap { thi =>
        val headPredicates = collection.mutable.Set.empty[Int]
        (if (_distinct) triples else distinct.triples).foldLeft(EvaluationResult(0, 0, 0)) { (result, triple) =>
          val (s, p, o) = (mapper.getIndexOpt(triple.subject), mapper.getIndexOpt(triple.predicate), mapper.getIndexOpt(triple.`object`))
          val isTrue = (s, p, o) match {
            case (Some(s), Some(p), Some(o)) if thi.predicates(p).subjects(s).contains(o) => true
            case _ => false
          }
          val fn = p match {
            case Some(p) if !headPredicates(p) =>
              headPredicates += p
              thi.predicates(p).size
            case _ => result.fn
          }
          EvaluationResult(
            if (isTrue) result.tp + 1 else result.tp,
            if (isTrue) result.fp else result.fp + 1,
            if (isTrue) fn - 1 else fn
          )
        }
      }
    }
  }

}

object PredictionResult {

  def apply(triples: Traversable[PredictedTriple], index: Index): PredictionResult = new PredictionResult(triples, index)

}