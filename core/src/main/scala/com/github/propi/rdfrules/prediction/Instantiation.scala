package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.algorithm.amie.{AtomCounting, VariableMap}
import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.{IndexPart, TripleIndex}
import com.github.propi.rdfrules.rule.InstantiatedAtom.PimpedAtom
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{InstantiatedAtom, InstantiatedRule}
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 14. 4. 2020.
  */
/*case class Instantiation(rule: FinalRule, paths: Ruleset) {

  def resolvedRule: ResolvedRule = {
    implicit val mapper: TripleItemIndex = paths.index.tripleItemMap
    rule
  }

  def triples(distinct: Boolean): ForEach[IndexItem.IntTriple] = {
    val col = paths.rules
      .flatMap(x => ForEach.from(x.body.iterator ++ Iterator(x.head)))
      .flatMap(x => ForEach.from(IndexItem(x)))
    if (distinct) col.distinct else col
  }

  def graph: Graph = {
    implicit val mapper: TripleItemIndex = paths.index.tripleItemMap
    Graph(triples(true).map(_.toTriple))
  }

}*/

object Instantiation {

  def resolvePredictionResult(s: Int, p: Int, o: Int)(implicit thi: TripleIndex[Int]): PredictedResult = {
    val predicateIndex = thi.predicates.get(p)
    val isPositive = predicateIndex.exists(_.subjects.get(s).exists(_.contains(o)))
    if (isPositive) {
      PredictedResult.Positive
    } else {
      val isCompletelyMissing = predicateIndex.exists { predicateIndex =>
        predicateIndex.higherCardinalitySide match {
          case TriplePosition.Subject => !predicateIndex.subjects.contains(s)
          case TriplePosition.Object => !predicateIndex.objects.contains(o)
        }
      }
      if (isCompletelyMissing) {
        PredictedResult.PcaPositive
      } else {
        PredictedResult.Negative
      }
    }
  }

  private def resolvePredictionResult(head: InstantiatedAtom)(implicit thi: TripleIndex[Int]): PredictedResult = resolvePredictionResult(head.subject, head.predicate, head.`object`)

  private def instantiateRule(rule: FinalRule, injectiveMapping: Boolean)(implicit ac: AtomCounting): Iterator[InstantiatedRule] = {
    val bodySet = rule.bodySet
    for {
      variableMap <- ac.paths(bodySet, VariableMap(injectiveMapping))
      instantiatedHead <- variableMap.specifyAtom(rule.head).toInstantiatedAtom
      instantiatedBody <- rule.body.foldLeft(Option(Vector.empty[InstantiatedAtom]))((atoms, atom) => atoms.flatMap(atoms => variableMap.specifyAtom(atom).toInstantiatedAtom.map(atoms :+ _)))
      predictionResult = resolvePredictionResult(instantiatedHead)(ac.tripleIndex)
    } yield {
      InstantiatedRule(instantiatedHead, instantiatedBody, predictionResult, rule)
    }
  }

  def apply(rules: ForEach[FinalRule], index: IndexPart, predictionResults: Set[PredictedResult], injectiveMapping: Boolean): ForEach[InstantiatedRule] = (f: InstantiatedRule => Unit) => {
    implicit val atomCounting: AtomCounting = AtomCounting()(index.tripleMap, index.tripleItemMap)
    for {
      rule <- rules
      iRule <- instantiateRule(rule, injectiveMapping)
      if predictionResults.isEmpty || predictionResults(iRule.predictedResult)
    } {
      f(iRule)
    }
  }

  /*def apply(rule: FinalRule, part: Part, index: Index, allowDuplicateAtoms: Boolean): Instantiation = {
    val coveredTriples = new ForEach[FinalRule] {
      def foreach(f: FinalRule => Unit): Unit = {
        val thi = index.tripleMap
        implicit val mapper: TripleItemIndex = index.tripleItemMap
        val atomCounting = new AtomCounting {
          implicit val tripleIndex: TripleIndex[Int] = thi
        }
        lazy val headAtoms = Set(rule.head)
        lazy val bodyAtoms = rule.body.toSet
        val (atoms, filter) = part match {
          case Part.Whole => (rule.body.toSet + rule.head) -> ((x: Iterator[atomCounting.VariableMap]) => x)
          case Part.Body(predictionType) =>
            predictionType match {
              case PredictionType.Existing => bodyAtoms -> ((x: Iterator[atomCounting.VariableMap]) => x.filter(atomCounting.exists(headAtoms, _)))
              case PredictionType.Missing => bodyAtoms -> ((x: Iterator[atomCounting.VariableMap]) => x.filterNot(atomCounting.exists(headAtoms, _)))
              case PredictionType.Complementary =>
                val predicateIndex = thi.predicates(rule.head.predicate)
                val isCompletelyMissing = predicateIndex.higherCardinalitySide match {
                  case TriplePosition.Subject => (atom: Atom) => !predicateIndex.subjects.contains(atom.subject.asInstanceOf[Atom.Constant].value)
                  case TriplePosition.Object => (atom: Atom) => !predicateIndex.objects.contains(atom.`object`.asInstanceOf[Atom.Constant].value)
                }
                bodyAtoms -> ((x: Iterator[atomCounting.VariableMap]) => x.filter(vm => isCompletelyMissing(vm.specifyAtom(rule.head))))
              case PredictionType.All => bodyAtoms -> ((x: Iterator[atomCounting.VariableMap]) => x)
            }
          case Part.Head => Set(rule.head) -> ((x: Iterator[atomCounting.VariableMap]) => x)
          case Part.HeadExisting => Set(rule.head) -> ((x: Iterator[atomCounting.VariableMap]) => x.filter(atomCounting.exists(bodyAtoms, _)))
          case Part.HeadMissing => Set(rule.head) -> ((x: Iterator[atomCounting.VariableMap]) => x.filterNot(atomCounting.exists(bodyAtoms, _)))
        }
        filter(atomCounting.paths(atoms, atomCounting.VariableMap(!allowDuplicateAtoms)))
          .map(variableMap => Rule(variableMap.specifyAtom(rule.head), rule.body.map(variableMap.specifyAtom))(TypedKeyMap()))
          .foreach(f)
      }
    }
    new Instantiation(rule, Ruleset(index, coveredTriples))
  }*/

}
