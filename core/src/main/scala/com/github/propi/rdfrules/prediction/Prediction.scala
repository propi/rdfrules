package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.algorithm.amie.{AtomCounting, VariableMap}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.{Index, IndexItem, TrainTestIndex, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Atom
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

object Prediction {

  def apply(rules: ForEach[FinalRule], train: Index, test: Option[Dataset], mergeTestAndTrainForPrediction: Boolean, predictionResults: Set[PredictedResult], injectiveMapping: Boolean)(implicit debugger: Debugger): PredictedTriples = {
    val index = test match {
      case Some(test) => TrainTestIndex(train, test)
      case None => TrainTestIndex(train)
    }
    val predictedTriples = new ForEach[PredictedTriple.Single] {
      def foreach(f: PredictedTriple.Single => Unit): Unit = {
        implicit val thi: TripleIndex[Int] = if (mergeTestAndTrainForPrediction) index.merged.tripleMap else index.test.tripleMap
        implicit val tii: TripleItemIndex = if (mergeTestAndTrainForPrediction) index.merged.tripleItemMap else index.test.tripleItemMap
        val atomCounting = AtomCounting()

        val res = rules.flatMap { rule =>
          val ruleBody = rule.bodySet
          val headVars = List(rule.head.subject, rule.head.`object`).collect {
            case x: Atom.Variable => x
          }
          val constantsToTriple: Seq[Atom.Constant] => IndexItem.IntTriple = (rule.head.subject, rule.head.`object`) match {
            case (_: Atom.Variable, _: Atom.Variable) => constants => IndexItem.Triple(constants.head.value, rule.head.predicate, constants.last.value)
            case (_: Atom.Variable, Atom.Constant(o)) => constants => IndexItem.Triple(constants.head.value, rule.head.predicate, o)
            case (Atom.Constant(s), _: Atom.Variable) => constants => IndexItem.Triple(s, rule.head.predicate, constants.head.value)
            case (Atom.Constant(s), Atom.Constant(o)) => _ => IndexItem.Triple(s, rule.head.predicate, o)
          }
          if (predictionResults.size == 1 && predictionResults(PredictedResult.Positive)) {
            atomCounting.specifyVariableMap(rule.head, VariableMap(injectiveMapping))
              .filter(atomCounting.exists(ruleBody, _))
              .map(variableMap => constantsToTriple(headVars.map(variableMap(_))))
              .map(x => PredictedTriple(x, PredictedResult.Positive, rule))
          } else {
            atomCounting
              .selectDistinctPairs(if (ruleBody.isEmpty) Set(rule.head) else ruleBody, headVars, Iterator(VariableMap(injectiveMapping)))
              .map(constantsToTriple)
              .map(x => PredictedTriple(x, Instantiation.resolvePredictionResult(x.s, x.p, x.o), rule))
              .filter(x => predictionResults.isEmpty || predictionResults(x.predictedResult))
          }
        }

        res.foreach(f)
      }
    }
    PredictedTriples(index, predictedTriples)
  }

}