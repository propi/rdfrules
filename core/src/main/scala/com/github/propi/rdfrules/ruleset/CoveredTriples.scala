package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.{Graph, Triple, TripleItem}
import com.github.propi.rdfrules.index.{Index, TripleHashIndex}
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 14. 4. 2020.
  */
case class CoveredTriples(rule: ResolvedRule, graph: Graph)

object CoveredTriples {

  def apply(rule: Rule.Simple, part: Part, index: Index, distinct: Boolean = true): CoveredTriples = {
    val coveredTriples = new Traversable[Triple] {
      def foreach[U](f: Triple => U): Unit = {
        index.tripleMap { thi =>
          index.tripleItemMap { implicit mapper =>
            val atomCounting = new AtomCounting {
              implicit val tripleIndex: TripleHashIndex[Int] = thi
            }
            val atoms = part match {
              case Part.Whole => rule.body :+ rule.head
              case Part.Body => rule.body
              case Part.Head => List(rule.head)
            }
            for {
              atom <- atoms
              p <- mapper.getTripleItemOpt(atom.predicate).collect {
                case x: TripleItem.Uri => x
              }
            } {
              atomCounting.getAtomTriples(atom).map {
                case (s, o) => mapper.getTripleItemOpt(s) -> mapper.getTripleItemOpt(o)
              }.collect {
                case (Some(s: TripleItem.Uri), Some(o)) => Triple(s, p, o)
              }.foreach(f(_))
            }
          }
        }
      }
    }
    index.tripleItemMap { implicit mapper =>
      new CoveredTriples(rule, Graph(if (distinct) coveredTriples.distinct else coveredTriples))
    }
  }

  sealed trait Part

  object Part {

    case object Head extends Part

    case object Body extends Part

    case object Whole extends Part

  }

}