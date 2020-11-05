package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.{Graph, TriplePosition}
import com.github.propi.rdfrules.index.{Index, IndexItem, TripleIndex}
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.rule.{Atom, Rule}
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 14. 4. 2020.
  */
case class CoveredPaths(rule: Rule.Simple, paths: Ruleset) {

  def resolvedRule: ResolvedRule = paths.index.tripleItemMap { implicit mapper =>
    rule
  }

  def triples(distinct: Boolean): Traversable[IndexItem.IntTriple] = {
    val col = paths.rules
      .view
      .flatMap(x => x.body.iterator ++ Iterator(x.head))
      .flatMap(IndexItem(_))
    if (distinct) col.distinct else col
  }

  def graph: Graph = paths.index.tripleItemMap { implicit mapper =>
    Graph(triples(true).view.map(_.toTriple), paths.isCached)
  }

}

object CoveredPaths {

  def apply(rule: Rule.Simple, part: Part, index: Index, allowDuplicateAtoms: Boolean): CoveredPaths = {
    val coveredTriples = new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = {
        index.tripleMap { thi =>
          index.tripleItemMap { implicit mapper =>
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
      }
    }
    new CoveredPaths(rule, Ruleset(index, coveredTriples, false))
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