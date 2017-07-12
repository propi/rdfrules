package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.rule.{Atom, Measure, Rule}

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val tripleIndex: TripleHashIndex

  implicit class PimpedRule(rule: Rule) {

    def withConfidence: Rule = {
      val bodySize = count(rule.body.toSet)
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      rule.measures +=(Measure.BodySize(bodySize), Measure.Confidence(support.toDouble / bodySize))
      rule
    }

    def withPcaConfidence: Rule = {
      val headInstances: Iterator[VariableMap] = rule.head match {
        case Atom(x: Atom.Variable, predicate, _) => tripleIndex.predicates(predicate).subjects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case Atom(_, predicate, x: Atom.Variable) => tripleIndex.predicates(predicate).objects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case _ => Iterator.empty
      }
      val bodySet = rule.body.toSet
      val pcaBodySize = headInstances.map(count(bodySet, _)).sum
      val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support].value
      rule.measures +=(Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(support.toDouble / pcaBodySize))
      rule
    }

  }

}
