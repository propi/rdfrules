package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.{Dataset, Graph, Triple}
import com.github.propi.rdfrules.index.Index

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
class PredictionResult private(_predictedTriples: Traversable[PredictedTriple], val index: Index) {

  def predictedTriples: TraversableView[PredictedTriple, Traversable[PredictedTriple]] = _predictedTriples.view

  def triples: Traversable[Triple] = predictedTriples.map(_.triple)

  def graph: Graph = Graph(triples)

  def mergedDataset: Dataset = index.toDataset + graph

  def evaluate: EvaluationResult = {
    index.tripleItemMap { mapper =>
      index.tripleMap { thi =>
        val headPredicates = collection.mutable.Set.empty[Int]
        triples.foldLeft(EvaluationResult(0, 0, 0)) { (result, triple) =>
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