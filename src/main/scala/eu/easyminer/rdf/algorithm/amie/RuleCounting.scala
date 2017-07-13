package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.{Atom, Measure, Rule}

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val tripleIndex: TripleHashIndex
  val minConfidence: Double

  implicit class PimpedRule(rule: Rule) {

    def withConfidence: Rule = {
      logger.debug(s"Confidence counting for rule: " + rule)
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      val bodySize = count(rule.body.toSet, support / minConfidence)
      rule.measures +=(Measure.BodySize(bodySize), Measure.Confidence(support.toDouble / bodySize))
      rule
    }

    //TODO nepocita s min confidence u count funkce
    def withPcaConfidence: Rule = {
      val headInstances: Iterator[VariableMap] = rule.head match {
        case Atom(x: Atom.Variable, predicate, _) => tripleIndex.predicates(predicate).subjects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case Atom(_, predicate, x: Atom.Variable) => tripleIndex.predicates(predicate).objects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case _ => Iterator.empty
      }
      val bodySet = rule.body.toSet
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      val pcaBodySize = headInstances.map(count(bodySet, support / minConfidence, _)).sum
      rule.measures +=(Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(support.toDouble / pcaBodySize))
      rule
    }

  }

}
