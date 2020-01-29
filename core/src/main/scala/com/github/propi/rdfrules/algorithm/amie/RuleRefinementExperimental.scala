package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.RuleFilter.{MinSupportRuleFilter, NoDuplicitRuleFilter, NoRepeatedGroups, RulePatternFilter}
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.Settings
import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.ExtendedRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.HowLong._
import com.github.propi.rdfrules.utils.extensions.IteratorExtension._

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleRefinementExperimental extends RuleRefinement {

  import settings._

  /**
    * Map of all rule predicates. Each predicate has subject and object variables.
    * For each this variable in a particular position it has list of other items (in subject or object position).
    * List of other items may contain both variables and constants.
    * Ex: predicate 1 -> ( subject a -> List(object b, object B1, object B2) )
    */
  private lazy val rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition, collection.Seq[Atom.Item]]] = {
    val map = collection.mutable.HashMap.empty[Int, collection.mutable.HashMap[TripleItemPosition, collection.mutable.ListBuffer[Atom.Item]]]
    for (atom <- Iterator(rule.head) ++ rule.body.iterator) {
      for ((position, item) <- Iterable[(TripleItemPosition, Atom.Item)](atom.subjectPosition -> atom.`object`, atom.objectPosition -> atom.subject) if position.item.isInstanceOf[Atom.Variable]) {
        map
          .getOrElseUpdate(atom.predicate, collection.mutable.HashMap.empty)
          .getOrElseUpdate(position, collection.mutable.ListBuffer.empty)
          .append(item)
      }
    }
    map
  }

  /**
    * Check whether fresh atom has been counted before the main mining stage or it should not be counted.
    * Counted fresh atoms may be atoms which have same predicate as any atom in the rule AND connecting item is on the same position as connecting atom item in the rule
    *
    * p(a, c) -> p(a, b) AND !instantiated THEN isCounted
    * p(a, c) -> p(a, B) AND !instantiated THEN isCounted
    * p(a, C) -> p(a, b) AND instantiated THEN isCounted
    * p(a, C) -> p(a, B) AND instantiated THEN !isCounted - COUNT IT!
    * p(a, b) -> p(a, b) AND !instantiated THEN isCounted
    * ( p(a, c) & p(b, c) -> p(a, b) NEBO p(a, c) & p(c, b) -> p(a, b) ) AND !instantiated THEN !isCounted - COUNT IT!
    * p(a, c) & p(c, a) -> p(a, B) AND !instantiated THEN !isCounted - COUNT IT!
    * other cases - COUNT IT!
    *
    * @param instantiated if true we want to create instantiated fresh atom
    * @param freshAtom    fresh atom to be checked
    * @param predicate    check for this predicate
    * @return true = fresh atom is counted (DONT COUNT IT!), false = fresh atom is not counted (COUNT IT!)
    */
  private def isCounted(freshAtom: FreshAtom, predicate: Int, instantiated: Boolean): Boolean = {
    //if the predicate is not valid by constraints OR is contained in the rule then the atom is counted (DONT COUNT IT!)
    !isValidPredicate(predicate) || rulePredicates.get(predicate).exists { predicate =>
      //if duplicit predicates are not allowed then then fresh atom is always counted and we dont count it!
      //we will find whether fresh atom should be counted, if yes then we return false (!isCounted - COUNT IT!)
      !withDuplicitPredicates || !((freshAtom.subject, freshAtom.`object`) match {
        //fresh atom is dangling and object is connector then we count this fresh atom only if:
        // - we want to create instantiated atom and all same redundant atoms have constants at the dangling position
        // - e.g.: p(a, B) -> p(a, C) we count it; p(a, B) -> p(a, b) we dont count it (redundant noisy atom)
        // - OR we want to create dangling atom and for this predicate there is an atom which has same object/subject (at a non-dangling position) as this fresh atom
        // - e.g.: p(a, c) -> p(a, b) we dont count it (it has been counted in countAtomsWithExistingPredicate); p(a, c) -> p(a, B) we dont count it (redundant noisy atom)
        // - e.g.: p(a, c) -> p(b, a) - count it!
        case (`dangling`, _) => if (instantiated) {
          predicate.get(freshAtom.objectPosition).forall(_.forall(_.isInstanceOf[Atom.Constant]))
        } else {
          // - e.g.: p(c, a) -> p(a, b) - count it!
          !predicate.contains(freshAtom.objectPosition)
        }
        case (_, `dangling`) => if (instantiated) {
          predicate.get(freshAtom.subjectPosition).forall(_.forall(_.isInstanceOf[Atom.Constant]))
        } else {
          // - e.g.: p(a, c) -> p(b, a) - count it!
          !predicate.contains(freshAtom.subjectPosition)
        }
        //fresh atom is closed then we count this fresh atom only if:
        // - we dont want to create instantiated atom (instantiated atom is possible only for dangling fresh atom)
        // - for this predicate there is an atom which has same subject as this fresh atom
        // - for each these atoms all objets must not equal fresh atom object
        // - OR for this predicate there is an atom which has same object...same bahaviour
        //shortly, if fresh atom is closed and has shared item with other atom in the rule in the same position then we count it unless there is a duplicit atom
        case _ => !instantiated && predicate.get(freshAtom.subjectPosition).forall(_.forall(_ != freshAtom.`object`)) // || predicate.get(freshAtom.objectPosition).exists(_.forall(_ != freshAtom.subject)))
      })
    }
  }

  /**
    * Check if a fresh atom is counted for both variable items and dangling constant item.
    * If fresh atom is counted for both variables and dangling constant, then it is counted, otherwise COUNT IT!
    *
    * @param freshAtom fresh atom
    * @param predicate checking for the predicate
    * @return true = is counted, false = it is not counted
    */
  private def isCounted(freshAtom: FreshAtom, predicate: Int): Boolean = {
    isCounted(freshAtom, predicate, false) && (!isWithInstances || isCounted(freshAtom, predicate, true))
  }

  /**
    * if all variables are known (instead of dangling nodes) we can enumerate all projections
    *
    * @param atom        countable fresh atom
    * @param variableMap variable map to constants
    */
  private def bindFreshAtom(atom: FreshAtom, variableMap: VariableMap): Iterator[Atom] = {
    //first we map all variables to constants
    (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
      //if there is variable on the subject place (it is dangling), count all dangling projections
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        //get all predicates for this object constant
        //skip all counted predicates that are included in this rule
        tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.iterator).flatMap { case (predicate, subjects) =>
          //if there exists atom in rule which has same predicate and object variable and is not instationed
          // - then we dont use this fresh atom for this predicate because p(a, B) -> p(a, b) is not allowed (redundant and noisy rule)
          val instantiated = if (isWithInstances && !onlyObjectInstances && !isCounted(atom, predicate, true)) {
            for (subject <- subjects.iterator) yield Atom(Atom.Constant(subject), predicate, atom.`object`)
          } else {
            Iterator.empty
          }
          //we dont count fresh atom only with variables if there exists atom in rule which has same predicate and object variable
          // - because for p(a, c) -> p(a, b) it is counted AND for p(a, c) -> p(a, B) it is forbidden combination (redundant and noisy rule)
          val logical = if (!isCounted(atom, predicate, false)) {
            Iterator(Atom(sv, predicate, atom.`object`))
          } else {
            Iterator.empty
          }
          instantiated ++ logical
        }
      //if there is variable on the object place (it is dangling), count all dangling projections
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        //get all predicates for this subject constant
        //skip all counted predicates that are included in this rule
        tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.iterator).flatMap { case (predicate, objects) =>
          val instantiated = if (isWithInstances && !isCounted(atom, predicate, true)) {
            for (_object <- objects.iterator) yield Atom(atom.subject, predicate, Atom.Constant(_object))
          } else {
            Iterator.empty
          }
          val logical = if (!isCounted(atom, predicate, false)) {
            Iterator(Atom(atom.subject, predicate, ov))
          } else {
            Iterator.empty
          }
          instantiated ++ logical
        }
      //all variables are constants, there are not any dangling variables - atom is closed; there is only one projection
      case (Atom.Constant(sc), Atom.Constant(oc)) =>
        //we skip counted predicate because in this case we count only with closed atom which are not counted for the existing predicate
        //we need to count all closed atoms
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatMap(_.iterator)) if !isCounted(atom, predicate, false)) yield {
          Atom(atom.subject, predicate, atom.`object`)
        }
      case _ => Iterator.empty
    }
  }

  def bindProjections(atoms: Set[Atom], headVars: Seq[Atom.Variable], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                     (implicit projections: mutable.HashMap[Atom, mutable.HashSet[Seq[Atom.Constant]]]): Unit = {
    if (countableFreshAtoms.nonEmpty) {
      val distinctPairs = howLong("select distinct pairs", true, forceShow = true) {
        selectDistinctPairs(atoms, headVars, variableMap).toList
      }
      for (newAtom <- countableFreshAtoms.iterator.flatMap(bindFreshAtom(_, variableMap))) {
        projections.getOrElseUpdate(newAtom, mutable.HashSet.empty) ++= distinctPairs
      }
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
        atomWithSpecifiedPredicate <- specifyAtom(freshAtom, variableMap) if !isCounted(freshAtom, atomWithSpecifiedPredicate.predicate)
        //check whether a predicate of new variable fresh atoms or instantiated fresh atoms has been counted in past (within duplicit predicates)
        //if yes we do not need to count projections for this predicate and fresh atom (for variable atoms or for instantiated atoms)
        notCountedInstanceProjections = !isCounted(freshAtom, atomWithSpecifiedPredicate.predicate, true)
        notCountedVariableProjections = !isCounted(freshAtom, atomWithSpecifiedPredicate.predicate, false)
        //for each predicate of the fresh atom we specify all variables and we filter projections which are connected with the rest of other atoms
        atom <- specifyAtom(atomWithSpecifiedPredicate, variableMap)
      } {
        val instantiated = if (isWithInstances && notCountedInstanceProjections) {
          //if we may to create instantiated atoms for this predicate and fresh atom (it is allowed and not already counted)
          //then we add new atom projection to atoms set
          //if onlyObjectInstances is true we do not project instance atom in the subject position
          if (freshAtom.subject == dangling && !onlyObjectInstances) {
            Iterator(atom.transform(`object` = freshAtom.`object`))
          } else if (freshAtom.`object` == dangling) {
            Iterator(atom.transform(subject = freshAtom.subject))
          } else {
            Iterator.empty
          }
        } else {
          Iterator.empty
        }
        //if we may to create variable atoms for this predicate and fresh atom (not already counted)
        //then we add new atom projection to atoms set
        val logical = if (notCountedVariableProjections) {
          Iterator(Atom(freshAtom.subject, atom.predicate, freshAtom.`object`))
        } else {
          Iterator.empty
        }
        if (instantiated.hasNext || logical.hasNext) {
          val distinctPairs = howLong("select distinct pairs", true, forceShow = true) {
            selectDistinctPairs(atoms, headVars, variableMap + (freshAtom.subject -> atom.subject.asInstanceOf[Atom.Constant], atom.predicate, freshAtom.`object` -> atom.`object`.asInstanceOf[Atom.Constant])).toList
          }
          for (newAtom <- instantiated ++ logical) {
            projections.getOrElseUpdate(newAtom, mutable.HashSet.empty) ++= distinctPairs
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
        val (countableAtoms, restAtoms) = otherFreshAtoms.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x)))
        for (variableMap <- specifyVariableMap(best, variableMap)) {
          //we specify best atom and specified variables (projection) send to the next iteration
          bindProjections(rest, headVars, restAtoms, countableAtoms, variableMap)
        }
      }
    }
  }

  override def refine: Iterator[ExtendedRule] = {
    val patternFilter = new RulePatternFilter(rule)
    if (rule.patterns.nonEmpty && !patternFilter.isDefined) {
      //if all paterns must be exact and are completely matched then we will not expand this rule
      Iterator.empty
    } else {
      //implicit val projections: mutable.HashMap[Atom, mutable.HashSet[Seq[Atom.Constant]]] = mutable.HashMap.empty
      //first we get all possible fresh atoms for the current rule (with one dangling variable or with two exising variables as closed rule)
      //we separate fresh atoms to two parts: countable and others (possible)
      //countable fresh atoms are atoms for which we know all existing variables (we needn't know dangling variable)
      // - then we can count all projections for this fresh atom and then only check existence of rest atoms in the rule
      //for other fresh atoms we need to find instances for unknown variables (not for dangling variable)
      val freshAtoms = getPossibleFreshAtoms.filter(x => patternFilter.matchFreshAtom(x)).toList
      val minSupport = math.max(rule.headSize * minHeadCoverage, settings.minSupport.getOrElse(1))
      val bodySet = rule.body.toSet
      val atoms = bodySet + rule.head
      val headVars = List(rule.head.subject, rule.head.`object`).collect {
        case x: Atom.Variable => x
      }
      val sameSupportAtoms = if (withDuplicitPredicates) freshAtoms.iterator.flatMap(countAtomsWithExistingPredicate) else Iterator.empty
      val addedAtoms = howLong("Rule expansion - count projections", true, forceShow = true) {
        implicit val projections: mutable.HashMap[Atom, mutable.HashSet[Seq[Atom.Constant]]] = mutable.HashMap.empty
        bindProjections(atoms, headVars, freshAtoms, Nil, new VariableMap(false))
        projections.mapValues(_.size).iterator
      }
      val ruleFilter = new MinSupportRuleFilter(minSupport) & new NoDuplicitRuleFilter(rule.head, bodySet) & new NoRepeatedGroups(withDuplicitPredicates, bodySet + rule.head, rulePredicates) & patternFilter
      (sameSupportAtoms ++ addedAtoms).map(x => x -> ruleFilter(x._1, x._2)).filter(_._2._1).map { case ((atom, support), (_, f)) =>
        val newRule = expand(atom, support)
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
      val danglings = if (!isWithInstances && rule.ruleLength + 1 >= maxRuleLength) {
        Iterator.empty
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
          val danglings = if (!isWithInstances && (rule.ruleLength + 1) >= maxRuleLength) {
            Iterator.empty
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

  private def countAtomsWithExistingPredicate(freshAtom: FreshAtom): Iterator[(Atom, Int)] = {

    /**
      * count all projections for fresh atom which is contained in the rule (only dangling atoms are supposed)
      *
      * @param variablePosition position of this variable (subject or object)
      */
    def countDanglingFreshAtom(variablePosition: TripleItemPosition) = {
      //filter for all atoms within the rule
      //atom subject = fresh atom subject OR atom object = fresh atom object
      val filterAtoms: Atom => Boolean = variablePosition match {
        case TripleItemPosition.Subject(s) => _.subject == s
        case TripleItemPosition.Object(o) => _.`object` == o
      }
      //get all atoms in the rule which satisfies the filter
      //get only atoms with distinct predicates
      (Iterator(rule.head) ++ rule.body.iterator).filter(filterAtoms).distinctBy(_.predicate).flatMap { atom =>
        //add this atom as a new projection with the same support as the current rule
        //if we want to create new atom with existing predicate then we may expand atom with variables only (not instantiated atom)
        // - only p(a, c) -> p(a, b) is allowed - not p(a, b) -> p(a, B), because it is noise = redundant pattern
        //if we want to create new instantiated atom with existing predicate then we may expand only other instantiated atom (not variable atom)
        // - only p(a, C) -> p(a, B) is allowed - not p(a, C) -> p(a, b), because it is noise = redundant pattern
        // - this phase (for instantioned atoms with existing predicates) is doing during the main count projections process
        if (atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable] && isValidPredicate(atom.predicate)) {
          Some(Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> rule.measures[Measure.Support].value)
        } else {
          None
        }
      }
    }

    freshAtom match {
      case FreshAtom(`dangling`, variable) => countDanglingFreshAtom(TripleItemPosition.Object(variable))
      case FreshAtom(variable, `dangling`) => countDanglingFreshAtom(TripleItemPosition.Subject(variable))
      case _ => Iterator.empty
    }
  }

}

object RuleRefinementExperimental {

  implicit class PimpedRule(extendedRule: ExtendedRule)(implicit tripleHashIndex: TripleHashIndex, settings: Settings) {
    def refine: Iterator[ExtendedRule] = {
      val _settings = settings
      new RuleRefinementExperimental {
        val rule: ExtendedRule = extendedRule
        val settings: Settings = _settings
        val tripleIndex: TripleHashIndex = implicitly[TripleHashIndex]
      }.refine
    }
  }

}