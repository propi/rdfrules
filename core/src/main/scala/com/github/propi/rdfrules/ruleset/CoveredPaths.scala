package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.{Graph, TriplePosition}
import com.github.propi.rdfrules.index.{Index, IndexItem, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.rule.{Atom, Rule}
import com.github.propi.rdfrules.utils.{ForEach, TypedKeyMap}

/**
  * Created by Vaclav Zeman on 14. 4. 2020.
  */
case class CoveredPaths(rule: Rule.Simple, paths: Ruleset) {

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

}

object CoveredPaths {

  def apply(rule: Rule.Simple, part: Part, index: Index, allowDuplicateAtoms: Boolean): CoveredPaths = {
    val coveredTriples = new ForEach[Rule.Simple] {
      def foreach(f: Rule.Simple => Unit): Unit = {
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
                val isCompletelyMissing = predicateIndex.mostFunctionalVariable match {
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
        filter(atomCounting.paths(atoms, new atomCounting.VariableMap(allowDuplicateAtoms)))
          .map(variableMap => Rule.Simple(variableMap.specifyAtom(rule.head), rule.body.map(variableMap.specifyAtom))(TypedKeyMap()))
          .foreach(f)
      }
    }
    new CoveredPaths(rule, Ruleset(index, coveredTriples))
  }

  sealed trait Part

  object Part {

    case object Head extends Part

    case object HeadExisting extends Part

    case object HeadMissing extends Part

    case class Body(predictionType: PredictionType) extends Part

    case object Whole extends Part

  }

}