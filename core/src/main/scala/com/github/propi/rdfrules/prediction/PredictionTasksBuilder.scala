package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.ops.TrainTestIndex
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.ForEach

sealed trait PredictionTasksBuilder

object PredictionTasksBuilder {

  sealed trait FromPredictedTriple extends PredictionTasksBuilder {
    def build(predictedTriple: PredictedTriple)(implicit index: TrainTestIndex): ForEach[PredictionTask]
  }

  sealed trait FromData extends PredictionTasksBuilder {
    def build(implicit index: TrainTestIndex): ForEach[PredictionTask]
  }

  object FromPredictedTriple {
    case class FromTargetVariablePosition(targetVariable: ConceptPosition) extends FromPredictedTriple {
      def build(predictedTriple: PredictedTriple)(implicit index: TrainTestIndex): ForEach[PredictionTask] = ForEach(PredictionTask(predictedTriple.triple, targetVariable))
    }

    case class FromPatterns(predictionTaskPatterns: Set[PredictionTaskPattern]) extends FromPredictedTriple {
      def build(predictedTriple: PredictedTriple)(implicit index: TrainTestIndex): ForEach[PredictionTask] = ForEach(
        PredictionTaskPattern.Mapped(predictedTriple.triple.p, TriplePosition.Subject),
        PredictionTaskPattern.Mapped(predictedTriple.triple.p, TriplePosition.Object)
      ).filter(predictionTaskPatterns.map(_.mapped(index.tripleItemMap))).flatMap(x => FromTargetVariablePosition(x.targetVariable).build(predictedTriple))
    }

    case object FromPredicateCardinalities extends FromPredictedTriple {
      def build(predictedTriple: PredictedTriple)(implicit index: TrainTestIndex): ForEach[PredictionTask] = ForEach(PredictionTask(predictedTriple.triple)(index.train.tripleMap))
    }
  }

  object FromTestSet {

    case class FromAll(injectiveMapping: Boolean) extends FromData {
      def build(implicit index: TrainTestIndex): ForEach[PredictionTask] = {
        FromTargetVariablePosition(TriplePosition.Object, injectiveMapping).build.concat(FromTargetVariablePosition(TriplePosition.Subject, injectiveMapping).build(index))
      }
    }

    case class FromTargetVariablePosition(targetVariable: ConceptPosition, injectiveMapping: Boolean) extends FromData {
      def build(implicit index: TrainTestIndex): ForEach[PredictionTask] = targetVariable match {
        case TriplePosition.Subject => index.test.tripleMap.predicates.pairIterator.flatMap(x => x._2.objects.pairIterator.filter(_._2.size(injectiveMapping) > 0).map(o => PredictionTask(x._1, TripleItemPosition.Object(o._1))))
        case TriplePosition.Object => index.test.tripleMap.predicates.pairIterator.flatMap(x => x._2.subjects.pairIterator.filter(_._2.size(injectiveMapping) > 0).map(s => PredictionTask(x._1, TripleItemPosition.Subject(s._1))))
      }
    }

    case class FromPatterns(predictionTaskPatterns: Set[PredictionTaskPattern], injectiveMapping: Boolean) extends FromData {
      def build(implicit index: TrainTestIndex): ForEach[PredictionTask] = {
        val mapped = predictionTaskPatterns.map(_.mapped(index.tripleItemMap))
        FromAll(injectiveMapping).build.filter(x => mapped(x.predictionTaskPattern))
      }
    }

    case class FromCustomSet(predictionTasks: Set[PredictionTask.Resolved], injectiveMapping: Boolean) extends FromData {
      def build(implicit index: TrainTestIndex): ForEach[PredictionTask] = ForEach.from(predictionTasks).map(_.mapped(index.tripleItemMap)).filter(!_.index(index.test.tripleMap).isEmpty(injectiveMapping))
    }

    case object FromPredicateCardinalities extends FromData {
      def build(implicit index: TrainTestIndex): ForEach[PredictionTask] = index.test.tripleMap.predicates.pairIterator.flatMap { case (p, pindex) =>
        val hcs = index.train.tripleMap.predicates.get(p).map(_.higherCardinalitySide).getOrElse(pindex.higherCardinalitySide)
        hcs match {
          case TriplePosition.Subject => pindex.subjects.iterator.map(s => PredictionTask(p, TripleItemPosition.Subject(s)))
          case TriplePosition.Object => pindex.objects.iterator.map(o => PredictionTask(p, TripleItemPosition.Object(o)))
        }
      }
    }
  }

}