package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.Graph
import com.github.propi.rdfrules.index.{Index, IndexItem, TripleIndex}
import com.github.propi.rdfrules.rule.Rule
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

  def apply(rule: Rule.Simple, part: Part, index: Index): CoveredPaths = {
    val coveredTriples = new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = {
        index.tripleMap { thi =>
          index.tripleItemMap { implicit mapper =>
            val atomCounting = new AtomCounting {
              implicit val tripleIndex: TripleIndex[Int] = thi
            }
            val /*(*/atoms/*, rest)*/ = part match {
              case Part.Whole => rule.body.toSet + rule.head// -> Set.empty[Atom]
              case Part.Body => rule.body.toSet// -> Set(rule.head)
              case Part.Head => Set(rule.head)// -> rule.body.toSet
            }
            atomCounting.paths(atoms, new atomCounting.VariableMap(true))
              //.filter(atomCounting.exists(rest, _))
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

    case object Body extends Part

    case object Whole extends Part

  }

}