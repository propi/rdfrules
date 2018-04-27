package com.github.propi.rdfrules.algorithm.dbscan

import com.github.propi.rdfrules.rule.{Atom, Measure, Rule}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait SimilarityCounting[-T] {

  def apply(v1: T, v2: T): Double

}

object SimilarityCounting {

  case class WeightedSimilarityCounting[T](w: Double, similarityCounting: SimilarityCounting[T])

  class Comb[T](similarityCountings: Vector[WeightedSimilarityCounting[T]]) extends SimilarityCounting[T] {

    private lazy val combSimilarities: (T, T) => Double = if (similarityCountings.iterator.map(_.w).sum == 1) {
      (v1, v2) => similarityCountings.iterator.map(x => x.w * x.similarityCounting(v1, v2)).sum
    } else {
      (v1, v2) => similarityCountings.iterator.map(_.similarityCounting(v1, v2)).sum / similarityCountings.size
    }

    def ~(weightedSimilarityCounting: WeightedSimilarityCounting[T]) = new Comb(similarityCountings :+ weightedSimilarityCounting)

    def apply(v1: T, v2: T): Double = combSimilarities(v1, v2)

  }

  implicit def weightedSimilarityCountingToComb[T](weightedSimilarityCounting: WeightedSimilarityCounting[T]): Comb[T] = new Comb(Vector(weightedSimilarityCounting))

  implicit class PimpedDouble(v: Double) {
    def *[T](similarityCounting: SimilarityCounting[T]): WeightedSimilarityCounting[T] = WeightedSimilarityCounting(v, similarityCounting)
  }

  object AtomsSimilarityCounting extends SimilarityCounting[Rule] {

    private def itemsSimilarity(p1: Int, p2: Int)(item1: Atom.Item, item2: Atom.Item) = (item1, item2) match {
      case (x: Atom.Variable, y: Atom.Variable) => if (x.index == y.index) 1.0 else if (p1 == p2) 0.5 else 0.0
      case (_: Atom.Variable, _: Atom.Constant) => if (p1 == p2) 0.5 else 0.0
      case (_: Atom.Constant, _: Atom.Variable) => if (p1 == p2) 0.5 else 0.0
      case (x: Atom.Constant, y: Atom.Constant) => if (x.value == y.value) 1.0 else 0.0
    }

    private def atomsSimilarity(atom1: Atom, atom2: Atom) = {
      val countItemsSimilarity = itemsSimilarity(atom1.predicate, atom2.predicate) _
      (if (atom1.predicate == atom2.predicate) 1.0 else 0.0) + countItemsSimilarity(atom1.subject, atom2.subject) + countItemsSimilarity(atom1.`object`, atom2.`object`)
    }

    def apply(rule1: Rule, rule2: Rule): Double = {
      val maxMatches = (math.max(rule1.body.size, rule2.body.size) + 1) * 3
      val (mainRule, secondRule) = if (rule1.body.size > rule2.body.size) (rule1, rule2) else (rule2, rule1)
      val similarity = (Iterator(mainRule.head) ++ mainRule.body.iterator).map { atom1 =>
        (Iterator(secondRule.head) ++ secondRule.body.iterator).map(atom2 => atomsSimilarity(atom1, atom2)).max
      }.sum
      similarity / maxMatches
    }

  }

  trait MeasuresSimilarityCounting extends SimilarityCounting[Rule] {

    protected def relativeNumbersSimilarity(v1: Double, v2: Double): Double = 1 - math.abs(v1 - v2)

    protected def absoluteNumbersSimilarity(v1: Int, v2: Int): Double = {
      val max = math.max(v1, v2)
      (max - math.abs(v1 - v2)) / max.toDouble
    }

  }

  object SupportSimilarityCounting extends MeasuresSimilarityCounting {

    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures[Measure.HeadCoverage].value, rule2.measures[Measure.HeadCoverage].value)

  }

  object ConfidenceSimilarityCounting extends MeasuresSimilarityCounting {

    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures.get[Measure.Confidence].map(_.value).getOrElse(0), rule2.measures.get[Measure.Confidence].map(_.value).getOrElse(0))

  }

  object LengthSimilarityCounting extends MeasuresSimilarityCounting {

    def apply(rule1: Rule, rule2: Rule): Double = absoluteNumbersSimilarity(rule1.ruleLength, rule2.ruleLength)

  }

}