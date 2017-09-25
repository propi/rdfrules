package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.{Atom, ExtendedRule, Measure}

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val tripleIndex: TripleHashIndex
  val minConfidence: Double

  implicit class PimpedRule(rule: ExtendedRule) {

    /**
      * Count confidence for the rule
      *
      * @return New rule with counted confidence
      */
    def withConfidence: ExtendedRule = {
      logger.debug(s"Confidence counting for rule: " + rule)
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      //we count body size, it is number of all possible paths for this rule from dataset only for body atoms
      //first we count body size threshold: support / minConfidence
      //it counts wanted body site. If the body size is greater than wanted body size then confidence will be always lower than our defined threshold (min confidence)
      val bodySize = count(rule.body.toSet, support / minConfidence)
      //confidence is number of head triples which are connected to other atoms in the rule DIVIDED number of all possible paths from body
      rule.measures += (Measure.BodySize(bodySize), Measure.Confidence(support.toDouble / bodySize))
      rule
    }

    //TODO nepocita s min confidence u count funkce
    def withPcaConfidence: ExtendedRule = {
      val headInstances: Iterator[VariableMap] = rule.head match {
        case Atom(x: Atom.Variable, predicate, _) => tripleIndex.predicates(predicate).subjects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case Atom(_, predicate, x: Atom.Variable) => tripleIndex.predicates(predicate).objects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case _ => Iterator.empty
      }
      val bodySet = rule.body.toSet
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      val pcaBodySize = headInstances.map(count(bodySet, support / minConfidence, _)).sum
      rule.measures += (Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(support.toDouble / pcaBodySize))
      rule
    }

  }

}
