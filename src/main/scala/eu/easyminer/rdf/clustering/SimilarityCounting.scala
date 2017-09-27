package eu.easyminer.rdf.clustering

import eu.easyminer.rdf.rule.{Atom, Measure, Rule}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait SimilarityCounting {

  def apply(rule1: Rule, rule2: Rule): Double

}

object SimilarityCounting {

  case class WeightedSimilarityCounting(w: Double, similarityCounting: SimilarityCounting)

  class Comb(similarityCountings: Vector[WeightedSimilarityCounting]) extends SimilarityCounting {

    private lazy val combSimilarities: (Rule, Rule) => Double = if (similarityCountings.iterator.map(_.w).sum == 1) {
      (rule1, rule2) => similarityCountings.iterator.map(x => x.w * x.similarityCounting(rule1, rule2)).sum
    } else {
      (rule1, rule2) => similarityCountings.iterator.map(_.similarityCounting(rule1, rule2)).sum / similarityCountings.size
    }

    def ~(weightedSimilarityCounting: WeightedSimilarityCounting) = new Comb(similarityCountings :+ weightedSimilarityCounting)

    def apply(rule1: Rule, rule2: Rule): Double = combSimilarities(rule1, rule2)

  }

  implicit def weightedSimilarityCountingToComb(weightedSimilarityCounting: WeightedSimilarityCounting): Comb = new Comb(Vector(weightedSimilarityCounting))

  implicit class PimpedDouble(v: Double) {
    def *(similarityCounting: SimilarityCounting) = WeightedSimilarityCounting(v, similarityCounting)
  }

  object AtomsSimilarityCounting extends SimilarityCounting {

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

  trait MeasuresSimilarityCounting extends SimilarityCounting {

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

    def apply(rule1: Rule, rule2: Rule): Double = relativeNumbersSimilarity(rule1.measures[Measure.Confidence].value, rule2.measures[Measure.Confidence].value)

  }

  object LengthSimilarityCounting extends MeasuresSimilarityCounting {

    def apply(rule1: Rule, rule2: Rule): Double = absoluteNumbersSimilarity(rule1.ruleLength, rule2.ruleLength)

  }

}
