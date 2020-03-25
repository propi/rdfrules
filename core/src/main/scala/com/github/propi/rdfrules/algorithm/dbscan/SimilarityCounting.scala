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

  implicit class PimpedDouble(val v: Double) extends AnyVal {
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
      //select mainRule and secondRule where mainRule.length >= secondRule.length
      val (mainRule, secondRule) = if (rule1.body.size > rule2.body.size) (rule1, rule2) else (rule2, rule1)
      //number of all atom items in longest rule
      val maxMatches = mainRule.ruleLength * 3
      //count all possible similarity combinations between atoms in rule1 and atoms in rule2
      //the result is matrix mainRule.length * secondRule.length
      val index = (Iterator(mainRule.head) ++ mainRule.body.iterator).map { atom1 =>
        (Iterator(secondRule.head) ++ secondRule.body.iterator).map(atom2 => atomsSimilarity(atom1, atom2)).toIndexedSeq
      }.toIndexedSeq
      //for each atom of the mainRule find max similarity with an atom of the secondRule
      val mainAtomMaxSims = index.map(_.max)
      //create a set of indices of the mainRule atoms for faster enum of rest atoms
      //val mainRuleIndices = index.indices.toSet
      //1. for the mainRule enum all combinations with length of the secondRule
      //e.g. for mainRule atoms (A, B, C) make combinations (AB, AC, BC)
      //2. for each combination make permutations
      //e.g. (AB, AC, BC) -> (AB, BA, AC, CA, BC, CB)
      //3. if the second rule atoms are (X, Y) then for each permutation count similarities and sum them
      //e.g. sim(A, X) + sim(B, Y), sim(B, X) + sim(A, Y), sim(A, X) + sim(C, Y), sim(C, X) + sim(A, Y), sim(B, X) + sim(C, Y), sim(C, X) + sim(B, Y)
      //4. at the each similarity of the each permutation add max similarity of the remaining atoms of the mainRule
      //e.g. sim(A, X) + sim(B, Y) + max(sim(C, X), sim(C, Y)), sim(B, X) + sim(A, Y) + max(sim(C, X), sim(C, Y)), sim(A, X) + sim(C, Y) + max(sim(B, X), sim(B, Y)), ...
      //5. finally select the max similarity from permutations
      val maxSimilarity = index.indices.combinations(secondRule.ruleLength).flatMap(_.permutations).map { mainPermutation =>
        val (rlist, msim) = mainPermutation.iterator.zipWithIndex.map {
          case (i, j) => i -> index(i)(j)
        }.foldLeft(List.empty[Int] -> 0.0) {
          case ((rlist, sum), (i, sim)) => (i :: rlist) -> (sum + sim)
        }
        val ssim = index.indices.iterator.filter(!rlist.contains(_)).map(mainAtomMaxSims(_)).sum
        msim + ssim
      }.max
      //to get range between 0-1 we need to normalize maxSimilarity by number of atom items in mainRule
      maxSimilarity / maxMatches
    }

  }

  trait MeasuresSimilarityCounting extends SimilarityCounting[Rule] {

    protected def relativeNumbersSimilarity(v1: Double, v2: Double): Double = 1 - math.abs(v1 - v2)

    protected def absoluteNumbersSimilarity(v1: Double, v2: Double): Double = {
      val max = math.max(math.abs(v1), math.abs(v2))
      if (max == 0) 1 else (max - math.abs(v1 - v2)) / max
    }

  }

  object SupportSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures[Measure.HeadCoverage].value, rule2.measures[Measure.HeadCoverage].value)
  }

  object ConfidenceSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures.get[Measure.Confidence].map(_.value).getOrElse(0), rule2.measures.get[Measure.Confidence].map(_.value).getOrElse(0))
  }

  object PcaConfidenceSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures.get[Measure.PcaConfidence].map(_.value).getOrElse(0), rule2.measures.get[Measure.PcaConfidence].map(_.value).getOrElse(0))
  }

  object LiftSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = absoluteNumbersSimilarity(rule1.measures.get[Measure.Lift].map(_.value).getOrElse(1), rule2.measures.get[Measure.Lift].map(_.value).getOrElse(1))
  }

  object LengthSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = absoluteNumbersSimilarity(rule1.ruleLength, rule2.ruleLength)
  }

}