package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.{Atom, DefaultConfidence, Measure, ResolvedAtom}
import com.github.propi.rdfrules.utils._

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait RuleCounting extends AtomCounting {

  val rule: FinalRule

  def hasQuasiBinding(injectiveMapping: Boolean): Boolean = hasQuasiBinding(rule.bodySet + rule.head, injectiveMapping)

  /**
    * Count confidence for the rule
    *
    * @param minConfidence minimal threshold for confidence counting (this restriction speed up computing)
    *                      rule with confidence lower than minConfidence will have confidence = minConfidence - 1
    * @return New rule with counted confidence
    */
  def withCwaConfidence(minConfidence: Double, injectiveMapping: Boolean, allPaths: Boolean = false)(implicit debugger: Debugger): FinalRule = {
    /*TODO: Check confidence counting - it may be wrong, e.g.: p(c, b) & p(c, a) => p(a, b)
    A -> C1 -> B
    A -> C2 -> B
    both are valid paths for the rule, but for (A, B) support = 1 and bodysize = 2; confidence = 0.5 - but it should be 1
    because C1 and C2 are connectable with A and B
    */
    //minimal allowed confidence is 0.1%
    if (minConfidence < 0.001) {
      withCwaConfidence(0.001, allPaths)
    } else {
      //logger.debug(s"Confidence counting for rule: " + rule)
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
        rule.withMeasures(rule.measures + Measure.BodySize(bodySize) + Measure.CwaConfidence(confidence))
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
  /*@deprecated("old lift measure")
  def withHeadConfidence: FinalRule = {
    //logger.debug(s"Head confidence counting for rule: " + rule)
    val average = (rule.head.subject, rule.head.`object`) match {
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
  }*/

  /**
    * Count lift for the rule.
    * Preconditions: Counted confidence and head confidence
    *
    * @return New rule with counted lift
    */
  def withLift(confidence: DefaultConfidence): FinalRule = {
    //logger.debug(s"Lift counting for rule: " + rule)
    (for {
      confidence <- confidence.confidenceOpt(rule.measures)
      propertyModeProbability <- tripleIndex.predicates.get(rule.head.predicate).flatMap { pindex =>
        (pindex.lowerCardinalitySide, rule.head.subject, rule.head.`object`) match {
          case (TriplePosition.Subject, Atom.Constant(x), _) => pindex.subjects.get(x).map(_.size(false).toDouble / pindex.size(false))
          case (TriplePosition.Object, _, Atom.Constant(x)) => pindex.objects.get(x).map(_.size(false).toDouble / pindex.size(false))
          case _ => Some(pindex.modeProbability)
        }
      }
    } yield {
      rule.withMeasures(rule.measures + Measure.Lift(confidence / propertyModeProbability))
    }).getOrElse(rule)
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
      withPcaConfidence(0.001, injectiveMapping, allPaths)
    } else {
      //logger.debug(s"PCA confidence counting for rule: " + rule)
      val bodySet = rule.bodySet
      val support = rule.support
      val maxPcaBodySize = (support / minPcaConfidence) + 1
      val zeroConstant = Atom.Constant(tripleItemIndex.zero)
      val emptyVariableMap = VariableMap(injectiveMapping)
      /*val headVars = List(rule.head.subject, rule.head.`object`).collect {
        case x: Atom.Variable => x
      }*/

      val isPCA: Seq[Atom.Constant] => Boolean = {
        val pindex = tripleIndex.predicates(rule.head.predicate)
        pindex.higherCardinalitySide match {
          case TriplePosition.Subject =>
            if (rule.head.subject.isInstanceOf[Atom.Constant]) {
              _ => true
            } else {
              constants => pindex.subjects.contains(constants.head.value)
            }
          case TriplePosition.Object =>
            if (rule.head.`object`.isInstanceOf[Atom.Constant]) {
              _ => true
            } else {
              constants => pindex.objects.contains(constants.last.value)
            }
        }
      }

      val (pcaVariableMaps, pairFilter): (Iterator[VariableMap], Option[Seq[Atom.Constant] => Boolean]) = {
        val pindex = tripleIndex.predicates(rule.head.predicate)
        lazy val _bestAtom = rule.body.iterator /*.filter(x => headVars.contains(x.subject) || headVars.contains(x.`object`))*/ .map(x => x -> scoreAtom(x, emptyVariableMap)).minBy(_._2)
        pindex.higherCardinalitySide match {
          case TriplePosition.Subject => rule.head.subject match {
            case _: Atom.Constant => Iterator(emptyVariableMap) -> None
            case v: Atom.Variable => _bestAtom match {
              case (_, score) if score < pindex.subjects.size => Iterator(emptyVariableMap) -> Some(isPCA)
              case _ => pindex.subjects.iterator.map(x => emptyVariableMap + (v -> Atom.Constant(x), rule.head.predicate, zeroConstant)) -> None
            }
          }
          case TriplePosition.Object => rule.head.`object` match {
            case _: Atom.Constant => Iterator(emptyVariableMap) -> None
            case v: Atom.Variable => _bestAtom match {
              case (_, score) if score < pindex.objects.size => Iterator(emptyVariableMap) -> Some(isPCA)
              case _ => pindex.objects.iterator.map(x => emptyVariableMap + (zeroConstant, rule.head.predicate, v -> Atom.Constant(x))) -> None
            }
          }
        }
      }
      val pcaBodySize = countDistinctPairs(bodySet, rule.head, maxPcaBodySize, pcaVariableMaps, pairFilter)
      val pcaConfidence = if (pcaBodySize == 0) 0.0 else support.toDouble / pcaBodySize
      if (pcaConfidence >= minPcaConfidence) {
        rule.withMeasures(rule.measures + Measure.PcaBodySize(pcaBodySize) + Measure.PcaConfidence(pcaConfidence))
      } else {
        rule
      }
    }
  }

  def withQpcaConfidence(minQpcaConfidence: Double, injectiveMapping: Boolean)(implicit debugger: Debugger): FinalRule = {
    //minimal allowed confidence is 0.1%
    if (minQpcaConfidence < 0.001) {
      withPcaConfidence(0.001, injectiveMapping)
    } else {
      val bodySet = rule.bodySet
      val support = rule.support
      val maxNegatives = (support / minQpcaConfidence) + 1
      var bodySize = 0
      val cache = collection.mutable.HashMap.empty[Int, IncrementalInt]
      val pindex = tripleIndex.predicates(rule.head.predicate)

      def incrementSubject(s: Int, o: Int): Unit = {
        val sindex = pindex.subjects.get(s)
        val currentCardinality = cache.getOrElseUpdate(s, IncrementalInt(sindex.map(_.size(injectiveMapping)).getOrElse(0)))
        if (sindex.exists(_.contains(o))) {
          bodySize += 1
        } else if (currentCardinality.getValue >= pindex.averageSubjectCardinality) {
          bodySize += 1
        } else {
          currentCardinality += 1
        }
      }

      def incrementObject(s: Int, o: Int): Unit = {
        val oindex = pindex.objects.get(o)
        val currentCardinality = cache.getOrElseUpdate(o, IncrementalInt(oindex.map(_.size(injectiveMapping)).getOrElse(0)))
        if (oindex.exists(_.contains(s))) {
          bodySize += 1
        } else if (currentCardinality.getValue >= pindex.averageObjectCardinality) {
          bodySize += 1
        } else {
          currentCardinality += 1
        }
      }

      val incrementNegatives: Seq[Atom.Constant] => Unit = {
        pindex.higherCardinalitySide match {
          case TriplePosition.Subject => rule.head.subject match {
            case Atom.Constant(s) => constants => incrementSubject(s, constants.head.value)
            case _ =>
              val objectConstant: Seq[Atom.Constant] => Int = rule.head.`object` match {
                case Atom.Constant(x) => _ => x
                case _ => _.last.value
              }
              constants => incrementSubject(constants.head.value, objectConstant(constants))
          }
          case TriplePosition.Object => rule.head.`object` match {
            case Atom.Constant(o) => constants => incrementObject(constants.head.value, o)
            case _ =>
              val subjectConstant: Seq[Atom.Constant] => Int = rule.head.subject match {
                case Atom.Constant(x) => _ => x
                case _ => _.head.value
              }
              constants => incrementObject(subjectConstant(constants), constants.last.value)
          }
        }
      }

      lazy val atomString = bodySet.iterator.map(ResolvedAtom(_)).map(Stringifier(_)).mkString(" ^ ")
      var thresholdTime = System.currentTimeMillis() + 30000
      var i = 0
      val predictions = selectDistinctPairs(bodySet, rule.head, Iterator(VariableMap(injectiveMapping)))
      while (predictions.hasNext && bodySize <= maxNegatives && !debugger.isInterrupted) {
        incrementNegatives(predictions.next())
        i += 1
        if (i % 500 == 0) {
          if (thresholdTime < System.currentTimeMillis()) {
            thresholdTime = System.currentTimeMillis() + 30000
            debugger.logger.info(s"Long counting of body size for: $atomString --- (${BigDecimal(i).toString} of ${BigDecimal(maxNegatives).toString} - ${BasicFunctions.round((i / maxNegatives) * 100, 2)}%)")
          }
        }
      }
      if (debugger.isInterrupted) bodySize = 0
      val qpcaConfidence = if (bodySize == 0) 0.0 else support.toDouble / bodySize
      //println(s"$atomString : $support / $bodySize = $qpcaConfidence")
      if (qpcaConfidence >= minQpcaConfidence) {
        rule.withMeasures(rule.measures + Measure.QpcaBodySize(bodySize) + Measure.QpcaConfidence(qpcaConfidence))
      } else {
        rule
      }
    }
  }

}

object RuleCounting {

  implicit class PimpedClosedRule(val rule: FinalRule)(implicit val tripleIndex: TripleIndex[Int], val tripleItemIndex: TripleItemIndex) extends RuleCounting

}