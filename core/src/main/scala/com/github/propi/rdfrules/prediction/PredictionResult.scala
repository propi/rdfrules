package com.github.propi.rdfrules.prediction

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
/*class PredictionResult private(val predictedTriples: ForEach[PredictedTriple], val index: Index, _distinct: Boolean = false) {

  def distinct: PredictionResult = new PredictionResult(predictedTriples.distinct, index, true)

  def onlyFunctionalProperties: PredictionResult = new PredictionResult(predictedTriples.distinctBy(x => x.triple.subject -> x.triple.predicate), index, true)

  def triples: ForEach[Triple] = predictedTriples.map(_.triple)

  def graph: Graph = if (_distinct) {
    Graph(triples)
  } else {
    Graph(distinct.triples)
  }

  def mergedDataset: Dataset = index.toDataset + graph

  def evaluate(withModel: Boolean = false): EvaluationResult = {
    val mapper = index.tripleItemMap
    val thi = index.tripleMap
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

}*/

object PredictionResult {

  //def apply(triples: ForEach[PredictedTriple], index: Index): PredictionResult = new PredictionResult(triples, index)

}
