package com.github.propi.rdfrules.algorithm.clustering

import com.github.propi.rdfrules.rule.Rule.FinalRule
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

  object AllAtomsSimilarityCounting extends AtomsSimilarityCounting(true)

  object BodyAtomsSimilarityCounting extends AtomsSimilarityCounting(false)

  class AtomsSimilarityCounting(withHead: Boolean) extends SimilarityCounting[Rule] {
    private def atomsSimilarity(atom1: Atom, atom2: Atom) = {
      val (psim, countItemsSimilarity): (Double, (Atom.Item, Atom.Item) => Double) = if (atom1.predicate == atom2.predicate) {
        1.0 -> {
          case (_: Atom.Variable, _: Atom.Variable) => 1.0
          case (_: Atom.Variable, _: Atom.Constant) => 0.5
          case (_: Atom.Constant, _: Atom.Variable) => 0.5
          case (x: Atom.Constant, y: Atom.Constant) => if (x.value == y.value) 1.0 else 0.0
        }
      } else {
        0.0 -> {
          case (x: Atom.Constant, y: Atom.Constant) => if (x.value == y.value) 1.0 else 0.0
          case _ => 0.0
        }
      }
      psim + countItemsSimilarity(atom1.subject, atom2.subject) + countItemsSimilarity(atom1.`object`, atom2.`object`)
    }

    def apply(rule1: Rule, rule2: Rule): Double = {
      //select mainRule and secondRule where mainRule.length >= secondRule.length
      val (mainRule, secondRule) = if (rule1.body.size > rule2.body.size) (rule1, rule2) else (rule2, rule1)
      //number of all atom items in longest rule
      val maxMatches = (secondRule.ruleLength - (if (withHead) 0 else 1)) * 3
      val headSimilarity = if (withHead) atomsSimilarity(mainRule.head, secondRule.head) else 0.0
      //count all possible similarity combinations between atoms in rule1 and atoms in rule2
      //the result is matrix mainRule.length * secondRule.length
      val index = Array.ofDim[Double](mainRule.body.length, secondRule.body.length)
      for {
        (atom1, i) <- mainRule.body.iterator.zipWithIndex
        (atom2, j) <- secondRule.body.iterator.zipWithIndex
      } {
        index(i)(j) = atomsSimilarity(atom1, atom2)
      }
      //      val index = (if (withHead) Iterator(mainRule.head) ++ mainRule.body.iterator else mainRule.body.iterator).map { atom1 =>
      //        (if (withHead) Iterator(secondRule.head) ++ secondRule.body.iterator else secondRule.body.iterator).map(atom2 => atomsSimilarity(atom1, atom2)).toIndexedSeq
      //      }.toIndexedSeq
      //for each atom of the mainRule find max similarity with an atom of the secondRule
      //val mainAtomMaxSims = index.map(_.max)
      //create a set of indices of the mainRule atoms for faster enum of rest atoms
      //val mainRuleIndices = index.indices.toSet
      //1. for the mainRule enum all combinations with length of the secondRule
      //e.g. for mainRule atoms (A, B, C) make combinations (AB, AC, BC)
      //2. for each combination make permutations
      //e.g. (AB, AC, BC) -> (AB, BA, AC, CA, BC, CB)
      //3. if the second rule atoms are (X, Y) then for each permutation count similarities and sum them
      //e.g. sim(A, X) + sim(B, Y), sim(B, X) + sim(A, Y), sim(A, X) + sim(C, Y), sim(C, X) + sim(A, Y), sim(B, X) + sim(C, Y), sim(C, X) + sim(B, Y)
      //4. at the each similarity of the each permutation add max similarity of the remaining atoms of the mainRule
      //NOW IT IS NOT USED!!! e.g. sim(A, X) + sim(B, Y) + max(sim(C, X), sim(C, Y)), sim(B, X) + sim(A, Y) + max(sim(C, X), sim(C, Y)), sim(A, X) + sim(C, Y) + max(sim(B, X), sim(B, Y)), ...
      //5. finally select the max similarity from permutations
      val maxSimilarity = index.indices.combinations(secondRule.body.length).flatMap(_.permutations).map { mainPermutation =>
        val /*(rlist, */ msim /*)*/ = mainPermutation.iterator.zipWithIndex.map {
          case (i, j) => /*i -> */ index(i)(j)
        }.sum /*.foldLeft(List.empty[Int] -> 0.0) {
          case ((rlist, sum), (i, sim)) => (i :: rlist) -> (sum + sim)
        }*/
        //val ssim = index.indices.iterator.filter(!rlist.contains(_)).map(mainAtomMaxSims(_)).sum
        //msim + ssim
        msim
      }.max
      //to get range between 0-1 we need to normalize maxSimilarity by number of atom items in mainRule
      (maxSimilarity + headSimilarity) / maxMatches
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
    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures.apply[Measure.HeadCoverage].value, rule2.measures.apply[Measure.HeadCoverage].value)
  }

  object ConfidenceSimilarityCounting extends MeasuresSimilarityCounting {
    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures.get[Measure.CwaConfidence].map(_.value).getOrElse(0), rule2.measures.get[Measure.CwaConfidence].map(_.value).getOrElse(0))
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

  implicit def ruleSimpleSimilarityCounting(implicit sc: SimilarityCounting[Rule]): SimilarityCounting[FinalRule] = sc

}