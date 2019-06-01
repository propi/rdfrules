package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.{Atom, Measure, Rule}
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val rule: Rule.Simple

  /**
    * Count confidence for the rule
    *
    * @param minConfidence minimal threshold for confidence counting (this restriction speed up computing)
    *                      rule with confidence lower than minConfidence will have confidence = minConfidence - 1
    * @return New rule with counted confidence
    */
  def withConfidence(minConfidence: Double, allPaths: Boolean = false): Rule.Simple = {
    /*TODO: Check confidence counting - it may be wrong, e.g.: p(c, b) & p(c, a) => p(a, b)
    A -> C1 -> B
    A -> C2 -> B
    both are valid paths for the rule, but for (A, B) support = 1 and bodysize = 2; confidence = 0.5 - but it should be 1
    because C1 and C2 are connectable with A and B
    */
    //minimal allowed confidence is 0.1%
    if (minConfidence < 0.001) {
      withConfidence(0.001, allPaths)
    } else {
      logger.debug(s"Confidence counting for rule: " + rule)
      val support = rule.measures[Measure.Support].value
      //we count body size, it is number of all possible paths for this rule from dataset only for body atoms
      //first we count body size threshold: support / minConfidence
      //it counts wanted body site. If the body size is greater than wanted body size then confidence will be always lower than our defined threshold (min confidence)
      val bodySize = if (allPaths) {
        count(rule.body.toSet, (support / minConfidence) + 1)
      } else {
        countDistinctPairs(rule.body.toSet, rule.head, (support / minConfidence) + 1)
      }
      //confidence is number of head triples which are connected to other atoms in the rule DIVIDED number of all possible paths from body
      val confidence = support.toDouble / bodySize
      if (confidence >= minConfidence) {
        rule.copy()(TypedKeyMap(Measure.BodySize(bodySize), Measure.Confidence(confidence)) ++= rule.measures)
      } else {
        rule
      }
    }
  }

  /**
    * Count head confidence for this rule.
    * Head confidence is average confidence using for lift counting
    *
    * @return New rule with counted head confidence
    */
  def withHeadConfidence: Rule.Simple = {
    logger.debug(s"Head confidence counting for rule: " + rule)
    val average = (rule.head.subject, rule.head.`object`) match {
      //TODO add functionality and inverseFunctionaly to choose which part of atom should take into account
      //if head is variables atom then average is number of all subjects (or objects depending on functionality) with the predicate of the head atom
      //DIVIDED
      //number of all subjects (or objects depending on functionality)
      case (_: Atom.Variable, _: Atom.Variable) =>
        if (inverseFunctionality(rule.head) > functionality(rule.head)) {
          tripleIndex.predicates(rule.head.predicate).objects.size.toDouble / tripleIndex.objects.size
        } else {
          tripleIndex.predicates(rule.head.predicate).subjects.size.toDouble / tripleIndex.subjects.size
        }
      //if head is instance atom then average is number of all possible triples for the head atom DIVIDED number of all subjects with the predicate of the head atom
      case (_: Atom.Variable, Atom.Constant(b)) => tripleIndex.predicates(rule.head.predicate).objects.get(b).map(_.size).getOrElse(0).toDouble / tripleIndex.predicates(rule.head.predicate).subjects.size
      case (Atom.Constant(a), _: Atom.Variable) => tripleIndex.predicates(rule.head.predicate).subjects.get(a).map(_.size).getOrElse(0).toDouble / tripleIndex.predicates(rule.head.predicate).objects.size
      case _ => 0
    }
    rule.copy()(TypedKeyMap(Measure.HeadConfidence(average)) ++= rule.measures)
  }

  /**
    * Count lift for the rule.
    * Preconditions: Counted confidence and head confidence
    *
    * @return New rule with counted lift
    */
  def withLift: Rule.Simple = {
    logger.debug(s"Lift counting for rule: " + rule)
    val confidence = rule.measures.get[Measure.Confidence]
    val average = rule.measures.get[Measure.HeadConfidence]
    (confidence, average) match {
      //lift is confidence DIVIDED average confidence for the head atom
      case (Some(confidence), Some(average)) => rule.copy()(TypedKeyMap(Measure.Lift(confidence.value / average.value)) ++= rule.measures)
      case _ => rule
    }
  }

  /**
    * Count pca confidence for the rule.
    * Pca confidence is less restrictive than the standard confidence.
    * If we count body size then we will be counting with head variables (instantiated) in subject position (or in object possition depending on functionality).
    * Thanks for this constraint we remove negative counterparts from body which can not be connected with head anymore.
    * p1(x, y) -> p2(x, y) if subject x within p1 predicate is not involved within p2 predicate, then we can remove p1(x, y) negative example
    * - then body size will be lower
    *
    * @param minPcaConfidence minimal threshold for pca confidence counting (this restriction speed up computing)
    *                         rule with pca confidence lower than minPcaConfidence will have confidence = minPcaConfidence - 1
    * @return New rule with counted pca confidence
    */
  def withPcaConfidence(minPcaConfidence: Double, allPaths: Boolean = false): Rule.Simple = {
    //minimal allowed confidence is 0.1%
    if (minPcaConfidence < 0.001) {
      withPcaConfidence(0.001, allPaths)
    } else {
      logger.debug(s"PCA confidence counting for rule: " + rule)
      /*
      //get all subject instances for head atoms
      val headInstances: Iterator[VariableMap] = (rule.head.subject, rule.head.`object`) match {
        //if subject is variable then we return all subjects for the head atom predicate
        case (x: Atom.Variable, _) => tripleIndex.predicates(rule.head.predicate).subjects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        //if subject is constant then we will search negative examples for object variable, we return all object instances for the atom predicate
        //TODO - this step is maybe invalid - only connections with subject are valid
        case (_, x: Atom.Variable) => tripleIndex.predicates(rule.head.predicate).objects.keysIterator.map(y => Map(x -> Atom.Constant(y)))
        case _ => Iterator.empty
      }*/

      val bodySet = rule.body.toSet
      val support = rule.measures[Measure.Support].value
      val maxPcaBodySize = (support / minPcaConfidence) + 1
      val newVar = (rule.body.iterator ++ Iterator(rule.head)).flatMap(x => Iterator(x.subject, x.`object`)).collect {
        case x: Atom.Variable => x
      }.max.++
      //if the inverse functionality is greater than functionality then we need to inverse the head atom
      val isInverseFunction = inverseFunctionality(rule.head) > functionality(rule.head)
      val existentialTriple = (rule.head.subject, rule.head.`object`) match {
        case (_: Atom.Variable, _: Atom.Variable) => if (isInverseFunction) {
          //for inverse functionality we predicate subject, object is fixed ?b
          rule.head.transform(subject = newVar)
        } else {
          //by default we predicate object, subject is fixed ?a
          rule.head.transform(`object` = newVar)
        }
        case (_: Atom.Variable, _) => rule.head.transform(subject = newVar)
        case (_, _: Atom.Variable) => rule.head.transform(`object` = newVar)
        case _ => rule.head
      }
      val pcaBodySize = if (allPaths) {
        count(bodySet + existentialTriple, maxPcaBodySize)
      } else {
        countDistinctPairs(bodySet + existentialTriple, rule.head, maxPcaBodySize)
      }
      /*val pcaBodySize = headInstances.foldLeft(0) { (pcaBodySize, variableMap) =>
        println(variableMap)
        //for each head instance we compute body size and sum it with previously counted body size
        //within each iteration we need to subtract the current pcaBodySize from maxPcaBodySize due to preservation of global threshold for summed paths (body sizes)
        val countForInstance = if (allPaths) {
          count(bodySet, maxPcaBodySize - pcaBodySize, variableMap)
        } else {
          countDistinctPairs(bodySet, headVars, maxPcaBodySize - pcaBodySize, variableMap)
        }
        println(countForInstance)
        if (countForInstance > 0) {
          RuleCounting.testCache += variableMap('a')
          //println("map: " + variableMap)
        }
        pcaBodySize + countForInstance
      }*/
      val pcaConfidence = support.toDouble / pcaBodySize
      if (pcaConfidence >= minPcaConfidence) {
        rule.copy()(TypedKeyMap(Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(pcaConfidence)) ++= rule.measures)
      } else {
        rule
      }
    }
  }

}

object RuleCounting {

  implicit class PimpedClosedRule(val rule: Rule.Simple)(implicit val tripleIndex: TripleHashIndex) extends RuleCounting

}