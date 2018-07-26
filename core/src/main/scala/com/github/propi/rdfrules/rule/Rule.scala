package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting._
import com.github.propi.rdfrules.utils.TypedKeyMap

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule {

  val body: IndexedSeq[Atom]
  val head: Atom
  val measures: TypedKeyMap.Immutable[Measure]

  lazy val ruleLength: Int = body.size + 1

  override def hashCode(): Int = {
    val support = measures[Measure.Support].value
    val headSize = measures[Measure.HeadSize].value
    val bodyHashCode = body.iterator.map { atom =>
      atom.predicate +
        (atom.subject match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        }) +
        (atom.`object` match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        })
    }.foldLeft(0)(_ ^ _)
    bodyHashCode + body.size * headSize + support
  }

}

object Rule {

  case class Simple(head: Atom, body: IndexedSeq[Atom])(val measures: TypedKeyMap.Immutable[Measure]) extends Rule

  object Simple {
    implicit def apply(extendedRule: ExtendedRule): Simple = new Simple(extendedRule.head, extendedRule.body)(extendedRule.measures)
  }

  implicit val ruleOrdering: Ordering[Rule] = Ordering.by[Rule, (Measure, Measure, Measure, Measure, Measure)] { rule =>
    (
      rule.measures.get(Measure.Cluster).getOrElse(Measure.Cluster(0)),
      rule.measures.get(Measure.PcaConfidence).getOrElse(Measure.PcaConfidence(0)),
      rule.measures.get(Measure.Lift).getOrElse(Measure.Lift(0)),
      rule.measures.get(Measure.Confidence).getOrElse(Measure.Confidence(0)),
      rule.measures(Measure.HeadCoverage)
    )
  }

  implicit val ruleSimpleOrdering: Ordering[Rule.Simple] = Ordering.by[Rule.Simple, Rule](_.asInstanceOf[Rule])

  implicit val ruleSimilarityCounting: SimilarityCounting[Rule.Simple] = (0.5 * AtomsSimilarityCounting) ~
    (0.1 * LengthSimilarityCounting) ~
    (0.15 * SupportSimilarityCounting) ~
    (0.05 * ConfidenceSimilarityCounting) ~
    (0.15 * PcaConfidenceSimilarityCounting) ~
    (0.05 * LiftSimilarityCounting)

}