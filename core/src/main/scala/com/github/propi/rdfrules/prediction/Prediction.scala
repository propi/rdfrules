package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.algorithm.amie.{AtomCounting, VariableMap}
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.data.{Dataset, TriplePosition}
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.index.ops.TrainTestIndex
import com.github.propi.rdfrules.rule.Atom
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

object Prediction {

  /**
    * We can make prediction faster if we focus only on prediction tasks which can cover a triple on the test set.
    */
  sealed trait HeadVariablePreMapping

  object HeadVariablePreMapping {
    case object NoMapping extends HeadVariablePreMapping

    /**
      * Rule: * => (?a p ?b) : ?a is lower cardinality site which should be predicted - (? p ?b) = prediction task
      * Before prediction we map the head variable ?b from test set, e.g., (?a p <o1-from-test>), (?a p <o2-from-test>)...
      */
    case object VariableMappingFromTestSetAtHigherCardinalitySite extends HeadVariablePreMapping

    case class VariableMappingFromTestSetAtCustomPosition(target: ConceptPosition) extends HeadVariablePreMapping
  }

  def apply(rules: ForEach[FinalRule],
            train: Index,
            test: Option[Dataset],
            mergeTestAndTrainForPrediction: Boolean,
            onlyTestCoveredPredictions: Boolean,
            predictionResults: Set[PredictedResult],
            injectiveMapping: Boolean,
            headVariablePreMapping: HeadVariablePreMapping)(implicit debugger: Debugger): PredictedTriples = {
    val index = test match {
      case Some(test) => TrainTestIndex(train.main, test, false)
      case None => TrainTestIndex(train)
    }
    implicit val thi: TripleIndex[Int] = if (mergeTestAndTrainForPrediction) index.merged.tripleMap else index.test.tripleMap
    implicit val tii: TripleItemIndex = if (mergeTestAndTrainForPrediction) index.merged.tripleItemMap else index.test.tripleItemMap
    val atomCounting = AtomCounting()
    val testIndex = index.test.tripleMap
    val testAtomCounting = if (!mergeTestAndTrainForPrediction) atomCounting else AtomCounting()(testIndex, index.test.tripleItemMap)

    val mapHead: Atom => Iterator[VariableMap] = headVariablePreMapping match {
      case HeadVariablePreMapping.NoMapping => _ => Iterator(VariableMap(injectiveMapping))
      case HeadVariablePreMapping.VariableMappingFromTestSetAtHigherCardinalitySite => atom => thi.predicates.get(atom.predicate).iterator.flatMap(_.higherCardinalitySide match {
        case TriplePosition.Subject => testAtomCounting.specifyVariableMapAtPosition(TriplePosition.Subject, atom, VariableMap(injectiveMapping))
        case TriplePosition.Object => testAtomCounting.specifyVariableMapAtPosition(TriplePosition.Object, atom, VariableMap(injectiveMapping))
      })
      case HeadVariablePreMapping.VariableMappingFromTestSetAtCustomPosition(position) => position match {
        case TriplePosition.Subject => atom => testAtomCounting.specifyVariableMapAtPosition(TriplePosition.Subject, atom, VariableMap(injectiveMapping))
        case TriplePosition.Object => atom => testAtomCounting.specifyVariableMapAtPosition(TriplePosition.Object, atom, VariableMap(injectiveMapping))
      }
    }

    def isInTest(s: Int, o: Int)(pi: TripleIndex.PredicateIndex[Int]): Boolean = pi.subjects.contains(s) || pi.objects.contains(o)

    val predictedTriples = new ForEach[PredictedTriple.Single] {
      def foreach(f: PredictedTriple.Single => Unit): Unit = {
        //val atomCountingForPositives = if (onlyTestCoveredPredictions) AtomCounting()(index.test.tripleMap, index.test.tripleItemMap) else atomCounting
        val res = rules.flatMap { rule =>
          val ruleBody = rule.bodySet
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
            .selectDistinctPairs(ruleBody, rule.head, mapHead(rule.head), pairFilter)
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