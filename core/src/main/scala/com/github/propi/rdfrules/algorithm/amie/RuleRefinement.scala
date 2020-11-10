package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.RuleFilter.{MinSupportRuleFilter, RuleConstraints, RulePatternFilter}
import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.ExtendedRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.{Debugger, IncrementalInt, TypedKeyMap}

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleRefinement extends AtomCounting with RuleExpansion {

  val rule: ExtendedRule
  val settings: RuleRefinement.Settings
  protected val debugger: Debugger

  import settings._

  /**
    * Next possible dangling variable for this rule
    */
  lazy val dangling: Atom.Variable = rule.maxVariable.++

  private lazy val minCurrentSupport = minComputedSupport(rule)

  /**
    * Map of all rule predicates. Each predicate has subject and object variables.
    * For each this variable in a particular position it has list of other items (in subject or object position).
    * List of other items may contain both variables and constants.
    * Ex: predicate 1 -> ( subject a -> List(object b, object B1, object B2) )
    */
  private lazy val rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition[Atom.Item], collection.Seq[Atom.Item]]] = {
    val map = collection.mutable.HashMap.empty[Int, collection.mutable.HashMap[TripleItemPosition[Atom.Item], collection.mutable.ListBuffer[Atom.Item]]]
    for (atom <- Iterator(rule.head) ++ rule.body.iterator) {
      for ((position, item) <- Iterable[(TripleItemPosition[Atom.Item], Atom.Item)](atom.subjectPosition -> atom.`object`, atom.objectPosition -> atom.subject) if position.item.isInstanceOf[Atom.Variable]) {
        map
          .getOrElseUpdate(atom.predicate, collection.mutable.HashMap.empty)
          .getOrElseUpdate(position, collection.mutable.ListBuffer.empty)
          .append(item)
      }
    }
    map
  }

  /**
    * This function returns true if the fresh atom is valid and should be added for other refining
    *
    * param instantiated if true we want to create instantiated fresh atom
    * param freshAtom    fresh atom to be checked
    * param predicate    check for this predicate
    * return true = fresh atom is valid (count it), false = fresh atom is not valid - is totally abandoned or is counted within fast support calculation for duplicit predicates (DONT COUNT IT!)
    */
  /*private def isValidFreshAtom(freshAtom: FreshAtom, predicate: Int, instantiated: Boolean): Boolean = {
    lazy val predicateIndex = tripleIndex.predicates(predicate)
    lazy val isFunctional = predicateIndex.isFunction
    lazy val isInverseFunctional = predicateIndex.isInverseFunction
    lazy val freshIsValidForDuplicitAtom: collection.Map[TripleItemPosition[Atom.Item], Seq[Atom.Item]] => Boolean = (freshAtom.subject, freshAtom.`object`) match {
      case (_, `dangling`) =>
        if (instantiated && !isFunctional) {
          //if we instantiate this atom and the predicate is not function then COUNT IT
          //because for (?a p C) => (?a p ?b) the C can be different for instantiated ?b
          _ => true
        } else {
          //otherwise we COUNT IT only if there is no (?a p ?) where (?a p ?) is the fresh atom
          //we DONT COUNT IT for any redundant atom since:
          //for !instantiated - is counted within fast support calculation for duplicit predicates: (?a p ?c) => (?a p ?b)
          //for instantiated - this should not be counted because p is function, therefore it would be redundant atom (?a p C) => (?a p ?b)
          predicateMap => !predicateMap.contains(freshAtom.subjectPosition.asInstanceOf[TripleItemPosition[Atom.Item]])
        }
      case (`dangling`, _) =>
        //same as above but for object
        if (instantiated && !isInverseFunctional) {
          _ => true
        } else {
          predicateMap => !predicateMap.contains(freshAtom.objectPosition.asInstanceOf[TripleItemPosition[Atom.Item]])
        }
      case _ =>
        if (isFunctional) {
          //we DONT COUNT redundant atoms if p is function: (?a p ?c) ^ (?b p ?c) => (?a p ?b)
          //atom (?a p ?c) is not considered, because during binding there are always duplicit triples.
          predicateMap => !predicateMap.contains(freshAtom.subjectPosition.asInstanceOf[TripleItemPosition[Atom.Item]])
        } else if (isInverseFunctional) {
          //we DONT COUNT redundant atoms if p is inverse function: (?c p ?a) ^ (?c p ?b) => (?a p ?b)
          //atom (?c p ?b) is not considered, because during binding there are always duplicit triples.
          predicateMap => !predicateMap.contains(freshAtom.objectPosition.asInstanceOf[TripleItemPosition[Atom.Item]])
        } else {
          //if p is not function or inverse function then it is always COUNTED unless there is an duplicit atom
          //(?a p ?c) ^ (?b p ?c) => (?a p ?b) - now it is valid because ?c can be different from ?b after binding
          //(?a p ?b) ^ (?b p ?a) => (?a p ?b) - duplicates are disabled
          predicateMap => !predicateMap.get(freshAtom.subjectPosition.asInstanceOf[TripleItemPosition[Atom.Item]]).exists(_.contains(freshAtom.`object`))
        }
    }
    //first we check whether the predicate is allowed.
    //if yes, then for each duplicit predicate in the rule we check whether there is no duplicity and redundancy withing refining
    isValidPredicate(predicate) && testAtomSize.forall(_ (predicateIndex.size)) && rulePredicates.get(predicate).forall { predicateMap =>
      withDuplicitPredicates && freshIsValidForDuplicitAtom(predicateMap)
    }
  }*/

  /**
    * Check if a fresh atom is counted for both variable items and dangling constant item.
    * If fresh atom is counted for both variables and dangling constant, then it is counted, otherwise COUNT IT!
    *
    * param freshAtom fresh atom
    * param predicate checking for the predicate
    * return true = is counted, false = it is not counted
    */
  /*private def isValidFreshAtom(freshAtom: FreshAtom, predicate: Int): Boolean = {
    isValidFreshAtom(freshAtom, predicate, false) && (!isWithInstances || isValidFreshAtom(freshAtom, predicate, true))
  }*/

  private def isUniquePredicate(predicate: Int): Boolean = !rulePredicates.contains(predicate)

  private def isDuplicateAtom(freshAtom: FreshAtom, predicate: Int): Boolean = {
    rulePredicates.get(predicate).exists(_.get(freshAtom.subjectPosition).exists(_.exists(_ == freshAtom.`object`)))
  }

  private def isValidFreshPredicate(freshAtom: FreshAtom, predicate: Int): Boolean = {
    lazy val predicateIndex = tripleIndex.predicates(predicate)
    isValidPredicate(predicate) &&
      testAtomSize.forall(_ (predicateIndex.size)) &&
      (if (withDuplicitPredicates) !isDuplicateAtom(freshAtom, predicate) else isUniquePredicate(predicate))
  }

  /**
    * From the current rule create new extended rules with all possible new atoms
    * New extended rules needn't be closed but contain maximal two dangling items.
    * One of two possible danglings is always within the last added atom.
    * Extended rules must fulfill all watching threshold conditions, constraints and patterns
    *
    * @return all extended rules with new atom
    */
  def refine: Iterator[ExtendedRule] = {
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
      val (countableFreshAtoms, possibleFreshAtoms) = getPossibleFreshAtoms.filter(x => patternFilter.matchFreshAtom(x)).toList.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      //val minSupport = minComputedSupport(rule)
      val bodySet = rule.body.toSet
      //maxSupport is variable where the maximal support from all extension rules is saved
      var maxSupport = 0
      //this function creates variable map with specified head variables
      val specifyHeadVariableMapWithAtom: (Int, Int) => VariableMap = {
        val specifyVariableMapWithAtom = specifyVariableMapForAtom(rule.head)
        (s, o) => specifyVariableMapWithAtom(rule.head.transform(subject = Atom.Constant(s), `object` = Atom.Constant(o)), new VariableMap(false))
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
      lazy val resolvedRule = ResolvedRule(rule.body.map(ResolvedRule.Atom(_)), rule.head)(TypedKeyMap(Measure.HeadSize(rule.headSize), Measure.Support(rule.support), Measure.HeadCoverage(rule.headCoverage)))
      var lastDumpDuration = currentDuration
      //howLong("Rule expansion - count projections", true) {
      rule.headTriples.zipWithIndex.takeWhile { x =>
        //if max support + remaining steps is lower than min support we can finish "count projection" process
        //example 1: head size is 10, min support is 5. Only 4 steps are remaining and there are no projection found then we can stop "count projection"
        // - because no projection can have support greater or equal 5
        //example 2: head size is 10, min support is 5, remaining steps 2, projection with maximal support has value 2: 2 + 2 = 4 it is less than 5 - we can stop counting
        val remains = headSize - x._2
        maxSupport + remains >= minCurrentSupport
      }.foreach { case ((_subject, _object), i) =>
        //for each triple covering head of this rule, find and count all possible projections for all possible fresh atoms
        val selectedAtoms = /*howLong("Rule expansion - bind projections", true)(*/ bindProjections(bodySet, possibleFreshAtoms, countableFreshAtoms, specifyHeadVariableMapWithAtom(_subject, _object)) //)
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
      val ruleFilter = new MinSupportRuleFilter(minCurrentSupport) /*& new NoDuplicitRuleFilter(rule.head, bodySet)*/ & patternFilter & new RuleConstraints(rule, filters)
      /*Iterator.continually(projections.headOption)
        .takeWhile(_.isDefined)
        .flatten*/
      projections.iterator
        .map { x =>
          //projections -= x._1
          x -> ruleFilter(x._1, x._2.getValue)
        }.filter(_._2._1)
        .map { case ((atom, support), (_, f)) =>
          val newRule = expand(atom, support.getValue)
          f.map(_ (newRule)).getOrElse(newRule)
        }
    }
  }

  /**
    * Create all possible combinations of fresh atoms which are connected to other rule atoms
    * Possible input/output
    * ClosedRule - result is one dangling atoms and closed atoms
    * OneDanglingRule - result is one dangling atoms and closed atoms
    * TwoDanglingsRule - result is two dangling atoms and closed atoms
    *
    * @return fresh atoms iterator
    */
  private def getPossibleFreshAtoms = rule match {
    case rule: ClosedRule =>
      //if the rule is closed then we connect all items with a new dangling atom
      // - result is one dangling rule
      //or we create new closed atoms which are created from all items combinations within this rule
      // - result is closed rule
      //condition for danglings: if not with instances and rule is closed and its lengths equals maxRuleLength - 1 then it should not be refined
      // - ex: p(b,a) => p(a,b) then p(a,c) ^ p(b,a) => p(a,b) is useless because it always generate dangling rules
      val danglings = if (rule.ruleLength + 1 >= maxRuleLength) {
        if (!isWithInstances) {
          Iterator.empty
        } else {
          constantsPosition match {
            case Some(ConstantsPosition.Subject) => rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x)))
            case Some(ConstantsPosition.Object) => rule.variables.iterator.map(x => Iterator(FreshAtom(x, dangling)))
            case _ => rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
          }
        }
      } else {
        rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
      }
      val closed = rule.variables.combinations(2).collect { case List(x, y) => Iterator(FreshAtom(x, y), FreshAtom(y, x)) }
      (danglings ++ closed).flatten
    case rule: DanglingRule =>
      rule.variables match {
        case ExtendedRule.OneDangling(dangling1, others) =>
          //if the rule has one dangling item then we create fresh atoms with this item and with a new dangling item
          // - result is one dangling rule again
          //or we create fresh atoms with existing dangling item with combination of other items
          // - result is closed rule
          //condition for danglings: if not with instances and rule is dangling and its lengths equals maxRuleLength - 1 then it should not be refined
          val danglings = if (rule.ruleLength + 1 >= maxRuleLength) {
            if (!isWithInstances) {
              Iterator.empty
            } else {
              constantsPosition match {
                case Some(ConstantsPosition.Subject) => Iterator(FreshAtom(dangling, dangling1))
                case Some(ConstantsPosition.Object) => Iterator(FreshAtom(dangling1, dangling))
                case _ => Iterator(FreshAtom(dangling1, dangling), FreshAtom(dangling, dangling1))
              }
            }
          } else {
            Iterator(FreshAtom(dangling1, dangling), FreshAtom(dangling, dangling1))
          }
          val closed = others.iterator.flatMap(x => Iterator(FreshAtom(dangling1, x), FreshAtom(x, dangling1)))
          danglings ++ closed
        case ExtendedRule.TwoDanglings(dangling1, dangling2, _) =>
          //if the rule has two dangling items then we create fresh atoms with these items and with a new dangling item
          // - result is two danglings rule again
          //or we create fresh atoms only with these dangling items
          // - result is closed rule
          //condition for danglings: if not with instances and rule is dangling and its lengths equals maxRuleLength - 1 then it should not be refined
          val danglings = if ((rule.ruleLength + 1) < maxRuleLength) rule.variables.danglings.iterator.flatMap(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling))) else Iterator.empty
          val closed = Iterator(FreshAtom(dangling1, dangling2), FreshAtom(dangling2, dangling1))
          danglings ++ closed
      }
  }

  /**
    * Create expanded projections from existing atoms in the rule.
    * There are some fresh atoms which can not decrease support: e.g. p(a, c) -> p(a, b)
    * -- same predicate AND subject or object within two atoms
    * For these new atoms we do not need to count support because it will be same
    * Only new variable atoms are added without counting - not instantiated atoms
    *
    * param freshAtom   any fresh atoms
    * param projections map of projections which can be fulfilled if there is some duplicits within this new fresh atom
    */
  /*private def countAtomsWithExistingPredicate(freshAtom: FreshAtom)
                                             (implicit projections: mutable.HashMap[Atom, IncrementalInt]): Unit = {

    /**
      * count all projections for fresh atom which is contained in the rule (only dangling atoms are supposed)
      *
      * @param variablePosition position of this variable (subject or object)
      */
    def countDanglingFreshAtom(variablePosition: TripleItemPosition[Atom.Item]) = {
      //filter for all atoms within the rule
      //atom subject = fresh atom subject OR atom object = fresh atom object
      val filterAtoms: Atom => Boolean = variablePosition match {
        case TripleItemPosition.Subject(s) => atom => atom.subject == s && !tripleIndex.predicates(atom.predicate).isFunction
        case TripleItemPosition.Object(o) => atom => atom.`object` == o && !tripleIndex.predicates(atom.predicate).isInverseFunction
      }
      //get all atoms in the rule which satisfies the filter
      //get only atoms with distinct predicates
      (Iterator(rule.head) ++ rule.body.iterator).filter(filterAtoms).distinctBy(_.predicate).foreach { atom =>
        //add this atom as a new projection with the same support as the current rule
        //if we want to create new atom with existing predicate then we may expand atom with variables only (not instantiated atom)
        // - only p(a, c) -> p(a, b) is allowed - not p(a, b) -> p(a, B), because it is noise = redundant pattern for functional p
        //if we want to create new instantiated atom with existing predicate then we may expand only other instantiated atom (not variable atom)
        // - only p(a, C) -> p(a, B) is allowed - not p(a, C) -> p(a, b), because it is noise = redundant pattern for functional p
        // - this phase (for instantioned atoms with existing predicates) is doing during the main count projections process
        if ( /*atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable] && */ isValidPredicate(atom.predicate)) {
          projections += (Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> IncrementalInt(rule.support))
        }
      }
      projections
    }

    freshAtom match {
      case FreshAtom(`dangling`, variable) => countDanglingFreshAtom(TripleItemPosition.Object(variable))
      case FreshAtom(variable, `dangling`) => countDanglingFreshAtom(TripleItemPosition.Subject(variable))
      case _ =>
    }
  }*/

  /**
    * Instantiated position
    * It returns Object for Nowhere constants position, it does not matter because instantiatedPosition is called always after isWithInstances.
    * It returns None if both Subject and Object are acceptable.
    *
    * @param predicate predicate
    * @return
    */
  private def instantiatedPosition(predicate: Int): Option[ConceptPosition] = constantsPosition.map {
    case ConstantsPosition.Object => TriplePosition.Object
    case ConstantsPosition.LeastFunctionalVariable => tripleIndex.predicates(predicate).leastFunctionalVariable
    case ConstantsPosition.Subject => TriplePosition.Subject
    case _ => TriplePosition.Object
  }

  /**
    * if minAtomSize is lower than 0 then the atom size must be greater than or equal to minCurrentSupport
    */
  private lazy val testAtomSize: Option[Int => Boolean] = {
    if (minAtomSize == 0) {
      None
    } else if (minAtomSize < 0) {
      Some((atomSize: Int) => atomSize >= minCurrentSupport)
    } else {
      Some((atomSize: Int) => atomSize >= minAtomSize)
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
  private def bindProjections(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                             (implicit projections: mutable.HashSet[Atom] = mutable.HashSet.empty): mutable.HashSet[Atom] = {
    //if there are some countable fresh atoms and there exists path for rest atoms then we can find all projections for these fresh atoms
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
        //for each predicate of the fresh atom we specify all variables and we filter projections which are connected with the rest of other atoms
        atom <- specifyAtom(atomWithSpecifiedPredicate, variableMap) if exists(atoms, variableMap + (freshAtom.subject -> atom.subject.asInstanceOf[Atom.Constant], atom.predicate, freshAtom.`object` -> atom.`object`.asInstanceOf[Atom.Constant]))
      } {
        if (isWithInstances) {
          //if we may to create instantiated atoms for this predicate and fresh atom (it is allowed and not already counted)
          //then we add new atom projection to atoms set
          //if onlyObjectInstances is true we do not project instance atom in the subject position
          val ip = instantiatedPosition(atom.predicate)
          if (freshAtom.subject == dangling && ip.forall(_ == TriplePosition.Subject) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).subjects(atom.subject.asInstanceOf[Atom.Constant].value).size))) projections += atom.transform(`object` = freshAtom.`object`)
          else if (freshAtom.`object` == dangling && ip.forall(_ == TriplePosition.Object) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).objects(atom.`object`.asInstanceOf[Atom.Constant].value).size))) projections += atom.transform(subject = freshAtom.subject)
        }
        //if we may to create variable atoms for this predicate and fresh atom (not already counted)
        //then we add new atom projection to atoms set
        projections += Atom(freshAtom.subject, atom.predicate, freshAtom.`object`)
      }
      //for other fresh atoms we need to specify best atom and repeat this function with all projections of the specified atom (recursively behaviour)
      if (otherFreshAtoms.nonEmpty) {
        //body atoms minus best atom are input to the recursive call of this function
        val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - best
        //if variables of best atom and other specified variables (in variableMap) coverage all variables of any fresh atom,
        //then this fresh atom will be immediately countable within next iteration
        //therefore in this step we can separate all fresh atoms to countable and other fresh atoms for next iteration
        val (countableAtoms, restAtoms) = otherFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x)))
        for (variableMap <- specifyVariableMap(best, variableMap)) {
          //we specify best atom and specified variables (projection) send to the next iteration
          bindProjections(rest, restAtoms, countableAtoms, variableMap)
        }
      }
    }
    projections
  }

  //TODO duplicate instantiated atoms checking needed during mapping where are atoms with contants
  private def isDuplicateInstantiatedAtom(p: Int, v: TripleItemPosition[Atom.Item], rest: Set[Atom], variableMap: VariableMap): (Atom.Constant, Atom.Constant) => Boolean = {
    val items = rulePredicates.get(p).flatMap(_.get(v)).getOrElse(Nil)
    val f = (s: Atom.Constant, o: Atom.Constant, x: Atom.Constant) => items.exists {
      case Atom.Constant(c) => x.value == c
      case y: Atom.Variable =>
        variableMap.get(y) match {
          case Some(Atom.Constant(c)) => x.value == c
          case None => !exists(rest, variableMap + (s, p, o))
        }
    }
    if (v.isInstanceOf[TripleItemPosition.Subject[Atom.Item]]) {
      (s, o) => f(s, o, o)
    } else {
      (s, o) => f(s, o, s)
    }
  }

  private def isDuplicateClosedAtom(s: Int, p: Int, o: Int, sv: TripleItemPosition[Atom.Item], ov: TripleItemPosition[Atom.Item]): Boolean = {
    val pr = rulePredicates.get(p)
    val f = (tip: TripleItemPosition[Atom.Item], x: Int) => pr.exists(_.get(tip).exists(_.exists {
      case Atom.Constant(c) => x == c
      case _ => false
    }))
    f(sv, o) || f(ov, s)
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
        for (predicate <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
          //if there exists atom in rule which has same predicate and object variable and is not instationed
          // - then we dont use this fresh atom for this predicate because p(a, B) -> p(a, b) is not allowed for functions (redundant and noisy rule)
          if (isWithInstances && instantiatedPosition(predicate).forall(_ == TriplePosition.Subject)) {
            val isDup = isDuplicateInstantiatedAtom(predicate, atom.objectPosition, rest, variableMap)
            for (subject <- tripleIndex.predicates(predicate).objects(oc).iterator.map(Atom.Constant) if !isDup(subject, o) && testAtomSize.forall(_ (tripleIndex.predicates(predicate).subjects(subject.value).size))) projections += Atom(subject, predicate, atom.`object`)
          }
          //we dont count fresh atom only with variables if there exists atom in rule which has same predicate and object variable
          // - because for p(a, c) -> p(a, b) it is counted AND for p(a, c) -> p(a, B) it is forbidden combination for functions (redundant and noisy rule)
          projections += Atom(sv, predicate, atom.`object`)
        }
      //if there is variable on the object place (it is dangling), count all dangling projections
      case (s@Atom.Constant(sc), ov: Atom.Variable) =>
        //get all predicates for this subject constant
        //skip all counted predicates that are included in this rule
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
          if (isWithInstances && instantiatedPosition(predicate).forall(_ == TriplePosition.Object)) {
            val isDup = isDuplicateInstantiatedAtom(predicate, atom.subjectPosition, rest, variableMap)
            for (_object <- tripleIndex.predicates(predicate).subjects(sc).iterator.map(Atom.Constant) if !isDup(s, _object) && testAtomSize.forall(_ (tripleIndex.predicates(predicate).objects(_object.value).size))) projections += Atom(atom.subject, predicate, _object)
          }
          projections += Atom(atom.subject, predicate, ov)
        }
      //all variables are constants, there are not any dangling variables - atom is closed; there is only one projection
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        //we skip counted predicate because in this case we count only with closed atom which are not counted for the existing predicate
        //we need to count all closed atoms
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatMap(_.iterator)) if isValidFreshPredicate(atom, predicate) && !isDuplicateClosedAtom(sc, predicate, oc, atom.subjectPosition, atom.objectPosition)) {
          projections += Atom(atom.subject, predicate, atom.`object`)
        }
      case _ =>
    }
  }

}

object RuleRefinement {

  class Settings(rulesMining: RulesMining)(implicit val debugger: Debugger, val mapper: TripleItemIndex) {
    @volatile private var _minHeadCoverage: Double = rulesMining.thresholds.get[Threshold.MinHeadCoverage].map(_.value).getOrElse(0.0)

    val parallelism: Int = rulesMining.parallelism
    val patterns: List[RulePattern.Mapped] = rulesMining.patterns.map(_.mapped)
    val minHeadSize: Int = rulesMining.thresholds.get[Threshold.MinHeadSize].map(_.value).getOrElse(100)
    val minSupport: Int = rulesMining.thresholds.get[Threshold.MinSupport].map(_.value).getOrElse(1)
    val minAtomSize: Int = rulesMining.thresholds.get[Threshold.MinAtomSize].map(_.value).getOrElse(0)
    val constantsPosition: Option[ConstantsPosition] = rulesMining.constraints.get[RuleConstraint.ConstantsAtPosition].map(_.position)
    val isWithInstances: Boolean = !constantsPosition.contains(ConstantsPosition.Nowhere)
    val maxRuleLength: Int = rulesMining.thresholds.get[Threshold.MaxRuleLength].map(_.value).getOrElse(3)
    val withDuplicitPredicates: Boolean = !rulesMining.constraints.exists[RuleConstraint.WithoutDuplicatePredicates]
    val filters: List[RuleConstraint.MappedFilter] = rulesMining.constraints.iterator.collect {
      case filter: RuleConstraint.Filter => filter.mapped
    }.toList
    val timeout: Option[Long] = rulesMining.thresholds.get[Threshold.Timeout].map(_.duration.toMillis)
    private val startTime = System.currentTimeMillis()

    def currentDuration: Long = System.currentTimeMillis() - startTime

    private val onlyPredicates = rulesMining.constraints.get[RuleConstraint.OnlyPredicates].map(_.mapped)
    private val withoutPredicates = rulesMining.constraints.get[RuleConstraint.WithoutPredicates].map(_.mapped)

    //def test(newAtom: Atom, rule: Option[Rule]): Boolean = filters.forall(_.test(newAtom, rule))

    //def test(newAtom: Atom, rule: Rule): Boolean = test(newAtom, Some(rule))

    def test(newAtom: Atom): Boolean = filters.forall(_.test(newAtom, None))

    def isValidPredicate(predicate: Int): Boolean = onlyPredicates.forall(_ (predicate)) && withoutPredicates.forall(!_ (predicate))

    def minHeadCoverage: Double = _minHeadCoverage

    def minComputedSupport(rule: ExtendedRule): Double = math.max(rule.headSize * minHeadCoverage, minSupport)

    def setMinHeadCoverage(value: Double): Unit = _minHeadCoverage = value

    override def toString: String = s"MinHeadSize=$minHeadSize,\n" +
      s"MinHeadCoverage=$minHeadCoverage,\n" +
      s"MinSupport=$minSupport,\n" +
      s"MaxThreads=$parallelism,\n" +
      s"MinAtomSize=$minAtomSize,\n" +
      s"MaxRuleLength=$maxRuleLength,\n" +
      s"WithConstants=$isWithInstances,\n" +
      s"ConstantsPosition=${constantsPosition.map(_.toString).getOrElse("All")},\n" +
      s"Timeout=${timeout.getOrElse(-1L)},\n" +
      s"WithDuplicitPredicates=$withDuplicitPredicates,\n" +
      s"Patterns=$patterns,\n" +
      s"OnlyPredicates=$onlyPredicates,\n" +
      s"WithoutPredicates=$withoutPredicates"
  }

  implicit class PimpedRule(extendedRule: ExtendedRule)(implicit tripleHashIndex: TripleIndex[Int], settings: Settings, _debugger: Debugger) {
    def refine: Iterator[ExtendedRule] = {
      val _settings = settings
      new RuleRefinement {
        val rule: ExtendedRule = extendedRule
        val settings: Settings = _settings
        val tripleIndex: TripleIndex[Int] = implicitly[TripleIndex[Int]]
        protected val debugger: Debugger = _debugger
      }.refine
    }
  }

}