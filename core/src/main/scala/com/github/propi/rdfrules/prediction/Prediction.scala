package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.algorithm.amie.{AtomCounting, VariableMap}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.index.ops.TrainTestIndex
import com.github.propi.rdfrules.rule.Atom
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

object Prediction {

  def apply(rules: ForEach[FinalRule], train: Index, test: Option[Dataset], mergeTestAndTrainForPrediction: Boolean, onlyTestCoveredPredictions: Boolean, predictionResults: Set[PredictedResult], injectiveMapping: Boolean)(implicit debugger: Debugger): PredictedTriples = {
    val index = test match {
      case Some(test) => TrainTestIndex(train.main, test, false)
      case None => TrainTestIndex(train)
    }
    val predictedTriples = new ForEach[PredictedTriple.Single] {
      def foreach(f: PredictedTriple.Single => Unit): Unit = {
        implicit val thi: TripleIndex[Int] = if (mergeTestAndTrainForPrediction) index.merged.tripleMap else index.test.tripleMap
        implicit val tii: TripleItemIndex = if (mergeTestAndTrainForPrediction) index.merged.tripleItemMap else index.test.tripleItemMap
        val atomCounting = AtomCounting()
        //val atomCountingForPositives = if (onlyTestCoveredPredictions) AtomCounting()(index.test.tripleMap, index.test.tripleItemMap) else atomCounting
        val testIndex = index.test.tripleMap

        def isInTest(s: Int, o: Int)(pi: TripleIndex.PredicateIndex[Int]): Boolean = pi.subjects.contains(s) || pi.objects.contains(o)

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
          val pairFilter: Option[Seq[Atom.Constant] => Boolean] = if (onlyTestCoveredPredictions) {
            val pindex = testIndex.predicates.get(rule.head.predicate)
            (rule.head.subject, rule.head.`object`) match {
              case (_: Atom.Variable, _: Atom.Variable) => Some(constants => pindex.exists(isInTest(constants.head.value, constants.last.value)))
              case (_: Atom.Variable, Atom.Constant(o)) => Some(constants => pindex.exists(isInTest(constants.head.value, o)))
              case (Atom.Constant(s), _: Atom.Variable) => Some(constants => pindex.exists(isInTest(s, constants.head.value)))
              case (Atom.Constant(s), Atom.Constant(o)) => Some(_ => pindex.exists(isInTest(s, o)))
            }
          } else {
            None
          }
          /*if (predictionResults.size == 1 && predictionResults(PredictedResult.Positive)) {
            atomCountingForPositives.specifyVariableMap(rule.head, VariableMap(injectiveMapping))
              .filter(atomCounting.exists(ruleBody, _))
              .map(variableMap => constantsToTriple(headVars.map(variableMap(_))))
              .map(x => PredictedTriple(x, PredictedResult.Positive, rule))
          } else {*/
          atomCounting
            .selectDistinctPairs(ruleBody, headVars, Iterator(VariableMap(injectiveMapping)), pairFilter)
            .map(constantsToTriple)
            .map(x => PredictedTriple(x, Instantiation.resolvePredictionResult(x.s, x.p, x.o), rule))
            .filter(x => predictionResults.isEmpty || predictionResults(x.predictedResult))
          //}
        }
        res.foreach(f)
      }
    }
    PredictedTriples(index, predictedTriples)
  }

}