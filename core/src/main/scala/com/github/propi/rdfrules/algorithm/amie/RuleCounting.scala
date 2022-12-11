package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{Atom, Measure}
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val rule: FinalRule

  def hasQuasiBinding(injectiveMapping: Boolean): Boolean = hasQuasiBinding(rule.bodySet, injectiveMapping)

  /**
    * Count confidence for the rule
    *
    * @param minConfidence minimal threshold for confidence counting (this restriction speed up computing)
    *                      rule with confidence lower than minConfidence will have confidence = minConfidence - 1
    * @return New rule with counted confidence
    */
  def withConfidence(minConfidence: Double, injectiveMapping: Boolean, allPaths: Boolean = false)(implicit debugger: Debugger): FinalRule = {
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
      val support = rule.support
      //we count body size, it is number of all possible paths for this rule from dataset only for body atoms
      //first we count body size threshold: support / minConfidence
      //it counts wanted body site. If the body size is greater than wanted body size then confidence will be always lower than our defined threshold (min confidence)
      val bodySize = if (allPaths) {
        count(rule.bodySet, (support / minConfidence) + 1, VariableMap(injectiveMapping))
      } else {
        countDistinctPairs(rule.bodySet, rule.head, (support / minConfidence) + 1, injectiveMapping)
      }
      //confidence is number of head triples which are connected to other atoms in the rule DIVIDED number of all possible paths from body
      val confidence = if (bodySize == 0) 0.0 else support.toDouble / bodySize
      if (confidence >= minConfidence) {
        rule.withMeasures(TypedKeyMap(Measure.BodySize(bodySize), Measure.Confidence(confidence)) ++= rule.measures)
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
  def withHeadConfidence: FinalRule = {
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
    rule.withMeasures(TypedKeyMap(Measure.HeadConfidence(average)) ++= rule.measures)
  }

  /**
    * Count lift for the rule.
    * Preconditions: Counted confidence and head confidence
    *
    * @return New rule with counted lift
    */
  def withLift: FinalRule = {
    logger.debug(s"Lift counting for rule: " + rule)
    val confidence = rule.measures.get[Measure.Confidence]
    val average = rule.measures.get[Measure.HeadConfidence]
    (confidence, average) match {
      //lift is confidence DIVIDED average confidence for the head atom
      case (Some(confidence), Some(average)) => rule.withMeasures(TypedKeyMap(Measure.Lift(confidence.value / average.value)) ++= rule.measures)
      case _ => rule
    }
  }

  /*private def bodySizeLowerBound(pca: Option[(Int, TripleItemPosition[Atom.Variable])], injectiveMapping: Boolean) = {
    if (rule.ruleLength == 3 && rule.head.subject.isInstanceOf[Atom.Variable] && rule.head.`object`.isInstanceOf[Atom.Variable]) {
    var predicate = Option.empty[Int]
    var sharedPosition = Option.empty[ConceptPosition]
    val headVars = Set(rule.head.subject.asInstanceOf[Atom.Variable], rule.head.`object`.asInstanceOf[Atom.Variable])
    val isAppropriateRule = rule.body.forall { atom =>
        atom.subject -> atom.`object` match {
          case (s: Atom.Variable, o: Atom.Variable) =>
            if (predicate.forall(_ == atom.predicate)) {
            if (headVars(s) && sharedPosition.forall(_ == TriplePosition.Object)) {
              predicate = Some(atom.predicate)
              sharedPosition = Some(TriplePosition.Object)
              true
            } else if (headVars(o) && sharedPosition.forall(_ == TriplePosition.Subject)) {
              predicate = Some(atom.predicate)
              sharedPosition = Some(TriplePosition.Subject)
              true
            } else {
              false
            }
            } else {
              false
            }
        }
      }
      if (isAppropriateRule) {
        val p = predicate.get
        pca match {
          case Some((headPredicate, headVar)) if injectiveMapping =>
          case Some((headPredicate, headVar)) =>
          case None if injectiveMapping =>
          case None =>
            sharedPosition.get match {
              case TriplePosition.Subject => tripleIndex.predicates(predicate.get).
              case TriplePosition.Object => tripleIndex.predicates(predicate.get).objects.valuesIterator.map(_.size(true))
            }

        }
      }
    if (predicates.size == 1) {
      for (shared <- variables.iterator.find(_._2 == atoms.size).map(_._1)) {
        variables.remove(shared)
        val checkExistence: (Int, TripleIndex.HashSet[Int] with TripleIndex.Reflexiveable) => Int = pca match {
          case Some((headPredicate, headVar)) if headVar.item == shared.item =>
            val pindex = tripleIndex.predicates(headPredicate)
            val pcaCheck: Int => Boolean = headVar match {
              case TripleItemPosition.Subject(_) => x => pindex.subjects.contains(x)
              case TripleItemPosition.Object(_) => x => pindex.objects.contains(x)
            }
            val reflexivityCheck: (Int, Int) => Boolean = if (injectiveMapping) (x, y) => x != y else (_, _) => true
            (x, index) => index.iterator.count(y => reflexivityCheck(x, y) && pcaCheck(y))
          case Some((headPredicate, headVar)) => (aConstant, index) => {
            val isValid = headVar match {
              case TripleItemPosition.Subject(_) => tripleIndex.predicates(headPredicate).subjects.contains(aConstant)
              case TripleItemPosition.Object(_) => tripleIndex.predicates(headPredicate).objects.contains(aConstant)
            }
            if (isValid) index.size(injectiveMapping) else 0
          }
          case None => (_, index) => index.size(injectiveMapping)
        }
        val pindex = tripleIndex.predicates(predicates.head)
        shared match {
          case TripleItemPosition.Subject(_) =>
            pindex.objects.pairIterator.map { case (o, subjects) => subjects.}
          case TripleItemPosition.Object(_) =>
        }
      }
      variables.filter.
    }
    val hmap = collection.mutable.HashMap.empty[Int, collection.mutable.ArrayBuffer[Atom]]
    for (atom <- atoms) {
      hmap.getOrElseUpdate(atom.predicate, collection.mutable.ArrayBuffer.empty).addOne(atom)
    }
    atoms.iterator.groupBy(_.predicate)(collection.mutable.ArrayBuffer).valuesIterator.filter(_.length > 1).foldLeft(variableMap) { case (variableMap, atoms) =>
      atoms.
    }
    hmap.valuesIterator.filter(_.)
    }
  }*/

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
  def withPcaConfidence(minPcaConfidence: Double, injectiveMapping: Boolean, allPaths: Boolean = false)(implicit debugger: Debugger): FinalRule = {
    //minimal allowed confidence is 0.1%
    if (minPcaConfidence < 0.001) {
      withPcaConfidence(0.001, allPaths)
    } else {
      logger.debug(s"PCA confidence counting for rule: " + rule)
      val bodySet = rule.bodySet
      val support = rule.support
      val maxPcaBodySize = (support / minPcaConfidence) + 1
      val zeroConstant = Atom.Constant(tripleItemIndex.zero)
      val emptyVariableMap = VariableMap(injectiveMapping)
      val pcaVariableMaps: Iterator[VariableMap] = {
        val pindex = tripleIndex.predicates(rule.head.predicate)
        pindex.higherCardinalitySide match {
          case TriplePosition.Subject =>
            if (rule.head.subject.isInstanceOf[Atom.Constant]) {
              Iterator(emptyVariableMap)
            } else {
              pindex.subjects.iterator.map(x => emptyVariableMap + (rule.head.subject.asInstanceOf[Atom.Variable] -> Atom.Constant(x), rule.head.predicate, zeroConstant))
            }
          case TriplePosition.Object =>
            if (rule.head.`object`.isInstanceOf[Atom.Constant]) {
              Iterator(emptyVariableMap)
            } else {
              pindex.objects.iterator.map(x => emptyVariableMap + (zeroConstant, rule.head.predicate, rule.head.`object`.asInstanceOf[Atom.Variable] -> Atom.Constant(x)))
            }
        }
      }
      val pcaBodySize = countDistinctPairs(bodySet, rule.head, maxPcaBodySize, pcaVariableMaps)
      val pcaConfidence = if (pcaBodySize == 0) 0.0 else support.toDouble / pcaBodySize
      if (pcaConfidence >= minPcaConfidence) {
        rule.withMeasures(TypedKeyMap(Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(pcaConfidence)) ++= rule.measures)
      } else {
        rule
      }
    }
  }

}

object RuleCounting {

  implicit class PimpedClosedRule(val rule: FinalRule)(implicit val tripleIndex: TripleIndex[Int], val tripleItemIndex: TripleItemIndex) extends RuleCounting

}