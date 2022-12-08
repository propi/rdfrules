package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.RuleFilter.{MinSupportRuleFilter, QuasiBindingFilter, RuleConstraints, RulePatternFilter}
import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{Debugger, IncrementalInt, TypedKeyMap}

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleRefinement extends RuleEnhancement with AtomCounting with RuleExpansion with FreshAtomGenerator {

  protected val debugger: Debugger

  import settings._

  /**
    * This is an auxiliary hmap used to prevent the generation of duplicate rules
    * The hmap represents all variables positions with predicate pointing to a max constant in a pair within an atom in the rule.
    * E.g. (a p C2) => (a p C1) : hmap = (a -> p) -> C2 if C2 >= C1
    * Shortly, constants must be ordered within same atoms
    */
  private lazy val maxConstants: collection.Map[(TripleItemPosition[Atom.Variable], Int), Int] = {
    val hmap = collection.mutable.HashMap.empty[(TripleItemPosition[Atom.Variable], Int), Int]
    for (atom <- rule.body) {
      val entry = (atom.subject, atom.`object`) match {
        case (x: Atom.Variable, Atom.Constant(c)) => Some((TripleItemPosition.Subject(x) -> atom.predicate) -> c)
        case (Atom.Constant(c), x: Atom.Variable) => Some((TripleItemPosition.Object(x) -> atom.predicate) -> c)
        case _ => None
      }
      for ((key, value) <- entry) {
        hmap.get(key) match {
          case Some(max) if max >= value =>
          case _ => hmap.put(key, value)
        }
      }
    }
    hmap
  }

  /**
    * Instantiated position
    * It returns Object for Nowhere constants position, it does not matter because instantiatedPosition is called always after isWithInstances.
    * It returns None if both Subject and Object are acceptable.
    */
  private lazy val instantiatedPosition: Int => Option[TriplePosition] = constantsPosition match {
    case Some(constantsPosition) => constantsPosition match {
      case ConstantsPosition.LowerCardinalitySide => predicate => Some(tripleIndex.predicates(predicate).lowerCardinalitySide)
      case ConstantsPosition.Subject =>
        val pos = Some(TriplePosition.Subject)
        _ => pos
      case _ =>
        val pos = Some(TriplePosition.Object)
        _ => pos
    }
    case None => _ => None
  }


  //var test = false

  /**
    * From the current rule create new extended rules with all possible new atoms
    * New extended rules needn't be closed but contain maximal two dangling items.
    * One of two possible danglings is always within the last added atom.
    * Extended rules must fulfill all watching threshold conditions, constraints and patterns
    *
    * @return all extended rules with new atom
    */
  def refine: Iterator[ExpandingRule] = {
    val patternFilter = new RulePatternFilter(rule, patterns, maxRuleLength)
    if (patterns.nonEmpty && !patternFilter.isDefined) {
      //if all paterns must be exact and are completely matched then we will not expand this rule
      Iterator.empty
    } else {
      implicit val projections: mutable.HashMap[Atom, IncrementalInt] = mutable.HashMap.empty
      //first we get all possible fresh atoms for the current rule (with one dangling variable or with two exising variables as closed rule)
      //we separate fresh atoms to two parts: countable and others (possible)
      //countable fresh atoms are atoms for which we know all existing variables (we needn't know dangling variable)
      // - then we can count all projections for this fresh atom and then only check existence of rest atoms in the rule
      //for other fresh atoms we need to find instances for unknown variables (not for dangling variable)
      /*if (rule.body.isEmpty && tripleItemIndex.getTripleItem(rule.head.predicate) == TripleItem.Uri("isLocatedIn") && rule.head.subject.isInstanceOf[Atom.Variable] && rule.head.`object`.isInstanceOf[Atom.Variable]) {
        println("je test")
        test = true
      }*/
      val freshAtoms: FreshAtoms = FreshAtoms.from(getPossibleFreshAtoms.filter(patternFilter.matchFreshAtom))(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      //val freshAtomsIndex: FreshAtoms.Index = FreshAtoms.indexFrom(freshAtoms)
      //val minSupport = minComputedSupport(rule)
      val bodySet = rule.bodySet
      //maxSupport is variable where the maximal support from all extension rules is saved
      var maxSupport = 0
      //this function creates variable map with specified head variables
      val specifyHeadVariableMapWithAtom: (Int, Int) => VariableMap = {
        val specifyVariableMapWithAtom = specifyVariableMapForAtom(rule.head)
        (s, o) => specifyVariableMapWithAtom(InstantiatedAtom(s, rule.head.predicate, o), VariableMap(injectiveMapping))
      }
      //debugger.debug("Rule expansion: " + rule, rule.headSize + 1) { ad =>
      //if (logger.underlying.isTraceEnabled) logger.trace("Rule expansion - " + rule + "\n" + "countable: " + countableFreshAtoms + "\n" + "possible: " + possibleFreshAtoms)
      //if duplicit predicates are allowed then
      //for all fresh atoms return all extensions with duplicit predicates by more efficient way
      //howLong("Rule expansion - count duplicit", true) {
      //TODO this is problem because we need to check atom duplicity
      //if (withDuplicitPredicates) (countableFreshAtoms.iterator ++ possibleFreshAtoms.iterator).foreach(countAtomsWithExistingPredicate)
      //}
      //ad.done()
      val headSize = rule.headSize
      lazy val resolvedRule = ResolvedRule(rule.body.map(ResolvedAtom(_)), rule.head)(TypedKeyMap(Measure.HeadSize(rule.headSize), Measure.Support(rule.support), Measure.HeadCoverage(rule.headCoverage)))
      var lastDumpDuration = currentDuration
      //howLong("Rule expansion - count projections", true) {
      rule.headTriples(injectiveMapping).zipWithIndex.takeWhile { x =>
        //if max support + remaining steps is lower than min support we can finish "count projection" process
        //example 1: head size is 10, min support is 5. Only 4 steps are remaining and there are no projection found then we can stop "count projection"
        // - because no projection can have support greater or equal 5
        //example 2: head size is 10, min support is 5, remaining steps 2, projection with maximal support has value 2: 2 + 2 = 4 it is less than 5 - we can stop counting
        val remains = headSize - x._2
        maxSupport + remains >= minCurrentSupport
      }.foreach { case ((_subject, _object), i) =>
        //for each triple covering head of this rule, find and count all possible projections for all possible fresh atoms
        val selectedAtoms = /*howLong("Rule expansion - bind projections", true)(*/ bindProjections(bodySet, freshAtoms.part1, freshAtoms.part2, specifyHeadVariableMapWithAtom(_subject, _object)) //)
        for (atom <- selectedAtoms) {
          //for each found projection increase support by 1 and find max support
          maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
        }
        val miningDuration = currentDuration
        if (miningDuration - lastDumpDuration > 30000) {
          debugger.logger.info(s"Long refining of rule $resolvedRule. Projections size: ${projections.size}. Step: $i of $headSize")
          lastDumpDuration = miningDuration
          if (timeout.exists(miningDuration >= _) || debugger.isInterrupted) {
            maxSupport = Int.MinValue
            projections.clear()
          }
        }
        //ad.done()
      }
      //}
      // }
      //if (logger.underlying.isTraceEnabled) logger.trace("Rule expansion " + Stringifier[Rule](rule) + ": total projections = " + projections.size)
      //filter all projections by minimal support and remove all duplicit projections
      //then create new rules from all projections (atoms)
      val ruleFilter = new MinSupportRuleFilter(minCurrentSupport) & patternFilter & new RuleConstraints(rule, filters) & new QuasiBindingFilter(bodySet, injectiveMapping, this)
      /*Iterator.continually(projections.headOption)
        .takeWhile(_.isDefined)
        .flatten*/
      projections.iterator.filter { case (atom, support) =>
        ruleFilter(atom, support.getValue)
      }.map { case (atom, support) =>
        expand(atom, support.getValue)
      }
    }
  }


  /**
    * Select all projections (atoms) from possible fresh atoms and one head specification (for one head specified atom/triple)
    * For each head triple we enumerate atoms from possible fresh atoms which are directly connected to this rule and fulfill all constraints
    *
    * @param atoms               all body atoms in the rule
    * @param possibleFreshAtoms  all possible fresh atoms which can be added to the rule and need to specify their variables by other atoms
    * @param countableFreshAtoms all possible fresh atoms which can be added to the rule and their projections can be counted immediately,
    *                            because we know all variables (are specified) of this new atom
    *                            (projected fresh atoms and their specified variables only need to check existence of the rest of other atoms within this rule)
    * @param variableMap         specified variables - it maps variables to constants
    * @param projections         set of atoms which are connectable to this rule and fulfill all constraints
    * @return set of projections/atoms
    */
  private def bindProjections(atoms: Set[Atom], countableFreshAtoms: FreshAtoms.Part1, possibleFreshAtoms: FreshAtoms.Part2, variableMap: VariableMap)
                             (implicit projections: mutable.HashSet[Atom] = mutable.HashSet.empty): mutable.HashSet[Atom] = {
    //if there are some countable fresh atoms and there exists path for rest atoms then we can find all projections for these fresh atoms
    //TODO exists function here is maybe redundant because we know that this mapping exists if variableMap constains such constants which were thruly mapped in the previous refining
    //TODO it requires we know all head triples which can not be bound to the body of the refining rule
    if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
      countableFreshAtoms.foreach(bindFreshAtom(_, atoms, variableMap))
    }
    //for each possible fresh we need to specify their variables
    if (possibleFreshAtoms.nonEmpty) {
      //select best atom and its score which has minimal projections from other atoms
      val (best, bestScore) = atoms.iterator.map(x => x -> scoreAtom(x, variableMap)).minBy(_._2)
      //select fresh atoms which have better score (lower) than best atom
      val (bestFreshAtoms, otherFreshAtoms) = possibleFreshAtoms.partition(scoreAtom(_, variableMap) <= bestScore)
      //for each this best fresh atom we directly specify all variables (enumerate all projections)
      //then for each projection we check existence of the rest of other atoms within this rule
      for {
        freshAtom <- bestFreshAtoms
        //specify fresh atom only with predicate
        //filter only atoms which need to be counted
        atomWithSpecifiedPredicate <- specifyAtom(freshAtom, variableMap) if isValidFreshPredicate(freshAtom, atomWithSpecifiedPredicate.predicate)
      } {
        val validAtoms = specifyAtom(atomWithSpecifiedPredicate, variableMap).filter(atom => exists(atoms, variableMap + (freshAtom.subject -> Atom.Constant(atom.subject), atom.predicate, freshAtom.`object` -> Atom.Constant(atom.`object`))))
        if (validAtoms.hasNext) {
          //if we may to create variable atoms for this predicate and fresh atom (not already counted)
          //then we add new atom projection to atoms set
          projections += Atom(freshAtom.subject, atomWithSpecifiedPredicate.predicate, freshAtom.`object`)
        }
        if (isWithInstances) {
          val ip = instantiatedPosition(atomWithSpecifiedPredicate.predicate)
          if (freshAtom.subject == dangling && ip.forall(_ == TriplePosition.Subject)) {
            val maxConstant = maxConstants.get(freshAtom.objectPosition -> atomWithSpecifiedPredicate.predicate)
            for (atom <- validAtoms if maxConstant.forall(atom.subject > _) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).subjects(atom.subject).size(injectiveMapping)))) {
              projections += Atom(Atom.Constant(atom.subject), atom.predicate, freshAtom.`object`)
            }
          } else if (freshAtom.`object` == dangling && ip.forall(_ == TriplePosition.Object)) {
            val maxConstant = maxConstants.get(freshAtom.subjectPosition -> atomWithSpecifiedPredicate.predicate)
            for (atom <- validAtoms if maxConstant.forall(atom.`object` > _) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).objects(atom.`object`).size(injectiveMapping)))) {
              projections += Atom(freshAtom.subject, atom.predicate, Atom.Constant(atom.`object`))
            }
          }
        }
      }
      //for other fresh atoms we need to specify best atom and repeat this function with all projections of the specified atom (recursively behaviour)
      if (otherFreshAtoms.nonEmpty) {
        //body atoms minus best atom are input to the recursive call of this function
        val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - best
        //if variables of best atom and other specified variables (in variableMap) coverage all variables of any fresh atom,
        //then this fresh atom will be immediately countable within next iteration
        //therefore in this step we can separate all fresh atoms to countable and other fresh atoms for next iteration
        val (countableAtoms, restAtoms) = otherFreshAtoms.partition(freshAtom => Iterator(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x)))
        for (variableMap <- specifyVariableMap(best, variableMap)) {
          //we specify best atom and specified variables (projection) send to the next iteration
          bindProjections(rest, countableAtoms, restAtoms, variableMap)
        }
      }
    }
    projections
  }

  private def isDuplicateInstantiatedAtom(p: Int, v: TripleItemPosition[Atom.Item], rest: Set[Atom], variableMap: VariableMap): (Atom.Constant, Atom.Constant) => Boolean = {
    if (withDuplicitPredicates) {
      val items = rulePredicates.get(p).flatMap(_.get(v)).getOrElse(Nil)
      val f = (s: Atom.Constant, o: Atom.Constant, x: Atom.Constant) => {
        var computeExpensiveExists = false
        (injectiveMapping && s == o) || items.exists {
          //(?a p C) ^ (?a p C) => (?a p1 X) - DUPLICATE!
          case Atom.Constant(c) => x.value == c
          case y: Atom.Variable if variableMap.injectiveMapping =>
            variableMap.get(y) match {
              //(?a p C) ^ (?a p ?b) => (?a p1 ?b) - VariableMap(?b -> C) - DUPLICATE!
              case Some(Atom.Constant(c)) => x.value == c
              //(?a p C) ^ (?a p ?b) => (?a p1 ?b) - VariableMap(?b -> NONE) - EXISTS(VariableMap(?b -> C)) - DUPLICATE!
              case None =>
                computeExpensiveExists = true
                false //IF NOT exists any other binding, e.g., (?a p C) ^ (?a p D), THEN it is DUPLICATE!
            }
          case _ => false
        } || (computeExpensiveExists && !exists(rest, variableMap + (s, p, o)))
      }
      if (v.isInstanceOf[TripleItemPosition.Subject[Atom.Item]]) {
        (s, o) => f(s, o, o)
      } else {
        (s, o) => f(s, o, s)
      }
    } else {
      (s, o) => if (injectiveMapping) s == o else false
    }
  }

  //(d p b) ^ (a p c) => (a p b)
  private def isDuplicateDanglingAtom(atom: Atom, rest: Set[Atom], variableMap: VariableMap): Boolean = {
    if (withDuplicitPredicates && injectiveMapping) {
      rulePredicates.get(atom.predicate).exists(_.get(if (atom.subject == dangling) atom.objectPosition else atom.subjectPosition).exists { _ =>
        specifyVariableMap(atom, variableMap).forall(!exists(rest, _))
      })
    } else {
      false
    }
  }

  private def isDuplicateClosedAtom(s: Int, p: Int, o: Int, sv: TripleItemPosition[Atom.Item], ov: TripleItemPosition[Atom.Item]): Boolean = {
    if (injectiveMapping && withDuplicitPredicates) {
      val pr = rulePredicates.get(p)
      val f = (tip: TripleItemPosition[Atom.Item], x: Int) => pr.exists(_.get(tip).exists(_.exists {
        //(?a p ?b) ^ (?a p C) => (?a p1 ?b) - VariableMap(?b -> C) - DUPLICATE!
        case Atom.Constant(c) => x == c
        case _ => false
      }))
      s == o || f(sv, o) || f(ov, s)
    } else {
      if (injectiveMapping) s == o else false
    }
  }

  /**
    * if all variables are known (instead of dangling nodes) we can enumerate all projections
    *
    * @param atom        countable fresh atom
    * @param variableMap variable map to constants
    * @param projections projections repository
    */
  private def bindFreshAtom(atom: FreshAtom, rest: Set[Atom], variableMap: VariableMap)
                           (implicit projections: mutable.HashSet[Atom]): Unit = {
    //first we map all variables to constants
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      //if there is variable on the subject place (it is dangling), count all dangling projections
      case (sv: Atom.Variable, o@Atom.Constant(oc)) =>
        //get all predicates for this object constant
        //skip all counted predicates that are included in this rule
        var hasValidConstantAtom = false
        for (predicate <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
          //if there exists atom in rule which has same predicate and object variable and is not instationed
          // - then we dont use this fresh atom for this predicate because p(a, B) -> p(a, b) is not allowed for functions (redundant and noisy rule)
          if (isWithInstances && instantiatedPosition(predicate).forall(_ == TriplePosition.Subject)) {
            //exists function is called before this method therefore for injectiveMapping we need to check whether a new atom is duplicated across rest atoms in the rule
            val isDup = isDuplicateInstantiatedAtom(predicate, atom.objectPosition, rest, variableMap)
            //a new constant C2 must be greater than possible constant C1 in the rule within a same pair of variable and predicate: E.g. (a p C2) => (a p C1) : hmap = (a -> p) -> C2 if C2 >= C1
            val maxConstant = maxConstants.get(atom.objectPosition -> predicate)
            val constants = tripleIndex.predicates(predicate).objects(oc).iterator.map(Atom.Constant).filter(subject => !isDup(subject, o) && maxConstant.forall(subject.value > _) && testAtomSize.forall(_ (tripleIndex.predicates(predicate).subjects(subject.value).size(injectiveMapping))))
            if (constants.hasNext) {
              hasValidConstantAtom = true
              constants.foreach(subject => projections += Atom(subject, predicate, atom.`object`))
            }
          }
          //we dont count fresh atom only with variables if there exists atom in rule which has same predicate and object variable
          // - because for p(a, c) -> p(a, b) it is counted AND for p(a, c) -> p(a, B) it is forbidden combination for functions (redundant and noisy rule)
          val logicAtom = Atom(sv, predicate, atom.`object`)
          //checking duplicity (non-injective mapping) in this place is probably more expensive than let non-injective mapping be added to projections
          //it does not matter because it is dangling, once the rule is closing then the non-injective mapping checking is performed.
          if (rule.ruleLength + 1 < maxRuleLength && (hasValidConstantAtom || !isDuplicateDanglingAtom(logicAtom, rest, variableMap))) projections += logicAtom
        }
      //if there is variable on the object place (it is dangling), count all dangling projections
      case (s@Atom.Constant(sc), ov: Atom.Variable) =>
        //get all predicates for this subject constant
        //skip all counted predicates that are included in this rule
        var hasValidConstantAtom = false
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
          if (isWithInstances && instantiatedPosition(predicate).forall(_ == TriplePosition.Object)) {
            //exists function is called before this method therefore for injectiveMapping we need to check whether a new atom is duplicated across rest atoms in the rule
            val isDup = isDuplicateInstantiatedAtom(predicate, atom.subjectPosition, rest, variableMap)
            //a new constant C2 must be greater than possible constant C1 in the rule within a same pair of variable and predicate: E.g. (a p C2) => (a p C1) : hmap = (a -> p) -> C2 if C2 >= C1
            val maxConstant = maxConstants.get(atom.subjectPosition -> predicate)
            val constants = tripleIndex.predicates(predicate).subjects(sc).iterator.map(Atom.Constant).filter(_object => !isDup(s, _object) && maxConstant.forall(_object.value > _) && testAtomSize.forall(_ (tripleIndex.predicates(predicate).objects(_object.value).size(injectiveMapping))))
            if (constants.hasNext) {
              hasValidConstantAtom = true
              constants.foreach(_object => projections += Atom(atom.subject, predicate, _object))
            }
          }
          val logicAtom = Atom(atom.subject, predicate, ov)
          if (rule.ruleLength + 1 < maxRuleLength && (hasValidConstantAtom || !isDuplicateDanglingAtom(logicAtom, rest, variableMap))) projections += logicAtom
        }
      //all variables are constants, there are not any dangling variables - atom is closed; there is only one projection
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        //we skip counted predicate because in this case we count only with closed atom which are not counted for the existing predicate
        //we need to count all closed atoms
        //exists function is called before this method therefore for injectiveMapping we need to check whether a new atom is duplicated across rest atoms in the rule
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatMap(_.iterator)) if isValidFreshPredicate(atom, predicate) && !isDuplicateClosedAtom(sc, predicate, oc, atom.subjectPosition, atom.objectPosition)) {
          projections += Atom(atom.subject, predicate, atom.`object`)
        }
      case _ =>
    }
  }

}

object RuleRefinement {

  implicit class PimpedRule(extendedRule: ExpandingRule)(implicit ti: TripleIndex[Int], tii: TripleItemIndex, settings: AmieSettings, _debugger: Debugger) {
    def refine: Iterator[ExpandingRule] = {
      val _settings = settings
      /*if (settings.experiment) {
        new RuleRefinement2 {
          val rule: ExpandingRule = extendedRule
          val settings: AmieSettings = _settings
          val tripleIndex: TripleIndex[Int] = implicitly[TripleIndex[Int]]
          val tripleItemIndex: TripleItemIndex = implicitly[TripleItemIndex]
          protected val debugger: Debugger = _debugger
        }.refine
      } else {*/
      new RuleRefinement {
        val rule: ExpandingRule = extendedRule
        val settings: AmieSettings = _settings
        val tripleIndex: TripleIndex[Int] = implicitly[TripleIndex[Int]]
        val tripleItemIndex: TripleItemIndex = implicitly[TripleItemIndex]
        protected val debugger: Debugger = _debugger
      }.refine
      //}
    }
  }

}