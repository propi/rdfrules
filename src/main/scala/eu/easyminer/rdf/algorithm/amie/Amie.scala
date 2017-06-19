package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.Atom.Variable
import eu.easyminer.rdf.rule.RuleConstraint.OnlyPredicates
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.BasicFunctions.Match

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePattern: Option[RulePattern], constraints: List[RuleConstraint]) {

  lazy val isWithInstances = constraints.contains(RuleConstraint.WithInstances)

  def addThreshold(threshold: Threshold) = {
    thresholds += (threshold.companion -> threshold)
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint) = new Amie(thresholds, rulePattern, ruleConstraint :: constraints)

  def setRulePattern(rulePattern: RulePattern) = new Amie(thresholds, Some(rulePattern), constraints)

  private def getHeadsFromRulePattern(rulePattern: RulePattern)(implicit tripleMap: TripleHashIndex.TripleMap) = {
    val atomIterator = rulePattern.consequent.predicate.map(Iterator(_)).getOrElse(tripleMap.keysIterator)
      .map(predicate => Atom(rulePattern.consequent.subject, predicate, rulePattern.consequent.`object`))
    if (rulePattern.consequent.subject.isInstanceOf[Variable] && rulePattern.consequent.`object`.isInstanceOf[Variable]) {
      atomIterator.flatMap { atom =>
        val it1 = tripleMap(atom.predicate).subjects.keysIterator.map(subject => Atom(Atom.Constant(subject), atom.predicate, atom.`object`))
        val it2 = tripleMap(atom.predicate).objects.keysIterator.map(`object` => Atom(atom.subject, atom.predicate, Atom.Constant(`object`)))
        Iterator(atom) ++ it1 ++ it2
      }
    } else {
      atomIterator
    }
  }

  def mineRules(implicit tripleMap: TripleHashIndex.TripleMap): Seq[ClosedRule] = {
    for (constraint <- constraints) Match(constraint) {
      case OnlyPredicates(predicates) => predicates.foreach(tripleMap -= _)
    }

  }

}

object Amie {

  def apply() = {
    val defaultThresholds: collection.mutable.Map[Threshold.Key, Threshold] = collection.mutable.HashMap(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, None, Nil)
  }

}