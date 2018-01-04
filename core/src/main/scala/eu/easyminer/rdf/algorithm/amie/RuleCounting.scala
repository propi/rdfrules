package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.Measure._
import eu.easyminer.rdf.rule.{Atom, Measure, Rule}

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val tripleIndex: TripleHashIndex

  implicit class PimpedRule(rule: Rule) {

    /**
      * Count confidence for the rule
      *
      * @param minConfidence minimal threshold for confidence counting (this restriction speed up computing)
      *                      rule with confidence lower than minConfidence will have confidence = minConfidence - 1
      * @return New rule with counted confidence
      */
    def withConfidence(minConfidence: Double): Rule = {
      logger.debug(s"Confidence counting for rule: " + rule)
      val support = rule.measures[Measure.Support].value
      //we count body size, it is number of all possible paths for this rule from dataset only for body atoms
      //first we count body size threshold: support / minConfidence
      //it counts wanted body site. If the body size is greater than wanted body size then confidence will be always lower than our defined threshold (min confidence)
      val bodySize = count(rule.body.toSet, (support / minConfidence) + 1)
      //confidence is number of head triples which are connected to other atoms in the rule DIVIDED number of all possible paths from body
      rule.measures += (Measure.BodySize(bodySize), Measure.Confidence(support.toDouble / bodySize))
      rule
    }

    /**
      * Count head confidence for this rule.
      * Head confidence is average confidence using for lift counting
      *
      * @return New rule with counted head confidence
      */
    def withHeadConfidence: Rule = {
      logger.debug(s"Head confidence counting for rule: " + rule)
      val average = rule.head match {
        //if head is variables atom then average is number of all subjects with the predicate of the head atom DIVIDED number of all subjects
        case Atom(_: Atom.Variable, p, _: Atom.Variable) => tripleIndex.predicates(p).subjects.size.toDouble / tripleIndex.subjects.size
        //if head is instance atom then average is number of all possible triples for the head atom DIVIDED number of all subjects with the predicate of the head atom
        case Atom(_: Atom.Variable, p, Atom.Constant(b)) => tripleIndex.predicates(p).objects.get(b).map(_.size).getOrElse(0).toDouble / tripleIndex.predicates(p).subjects.size
        case Atom(Atom.Constant(a), p, _: Atom.Variable) => tripleIndex.predicates(p).subjects.get(a).map(_.size).getOrElse(0).toDouble / tripleIndex.predicates(p).objects.size
        case _ => 0
      }
      rule.measures += Measure.HeadConfidence(average)
      rule
    }

    /**
      * Count lift for the rule.
      * Preconditions: Counted confidence and head confidence
      *
      * @return New rule with counted lift
      */
    def withLift: Rule = {
      logger.debug(s"Lift counting for rule: " + rule)
      val confidence = rule.measures[Measure.Confidence].value
      val average = rule.measures[Measure.HeadConfidence].value
      //lift is confidence DIVIDED average confidence for the head atom
      rule.measures += Measure.Lift(confidence / average)
      rule
    }

    /**
      * Count pca confidence for the rule.
      * Pca confidence is less restrictive than the standard confidence.
      * If we count body size then we will be counting with head variables (instantiated) in subject position.
      * Thanks for this constraint we remove negative counterparts from body which can not be connected with head anymore.
      * p1(x, y) -> p2(x, y) if subject x within p1 predicate is not involved within p2 predicate, then we can remove p1(x, y) negative example
      * - then body size will be lower
      *
      * @param minPcaConfidence minimal threshold for pca confidence counting (this restriction speed up computing)
      *                         rule with pca confidence lower than minPcaConfidence will have confidence = minPcaConfidence - 1
      * @return New rule with counted pca confidence
      */
    def withPcaConfidence(minPcaConfidence: Double): Rule = {
      logger.debug(s"Pca confidence counting for rule: " + rule)
      //get all subject instances for head atoms
      val headInstances: Iterator[VariableMap] = rule.head match {
        //if subject is variable then we return all subjects for the head atom predicate
        case Atom(x: Atom.Variable, predicate, _) => tripleIndex.predicates(predicate).subjects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        //if subject is constant then we will search negative examples for object variable, we return all object instances for the atom predicate
        case Atom(_, predicate, x: Atom.Variable) => tripleIndex.predicates(predicate).objects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case _ => Iterator.empty
      }
      val bodySet = rule.body.toSet
      val support = rule.measures[Measure.Support].value
      val maxPcaBodySize = (support / minPcaConfidence) + 1
      val pcaBodySize = headInstances.foldLeft(0) { (pcaBodySize, variableMap) =>
        //for each head instance we compute body size and sum it with previously counted body size
        //within each iteration we need to subtract the current pcaBodySize from maxPcaBodySize due to preservation of global threshold for summed paths (body sizes)
        pcaBodySize + count(bodySet, maxPcaBodySize - pcaBodySize, variableMap)
      }
      rule.measures += (Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(support.toDouble / pcaBodySize))
      rule
    }

    /**
      * Count pca lift for the rule.
      * Preconditions: Counted pca confidence and head confidence
      *
      * @return New rule with counted pca lift
      */
    def withPcaLift: Rule = {
      logger.debug(s"Pca lift counting for rule: " + rule)
      val pcaConfidence = rule.measures[Measure.PcaConfidence].value
      val average = rule.measures[Measure.HeadConfidence].value
      //lift is confidence DIVIDED average confidence for the head atom
      rule.measures += Measure.PcaLift(pcaConfidence / average)
      rule
    }

  }

}
