package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.algorithm.amie.RuleExpansion._
import eu.easyminer.rdf.algorithm.amie.RuleFilter.{MinSupportRuleFilter, NoDuplicitRuleFilter, NoRepeatedGroups}
import eu.easyminer.rdf.data.{IncrementalInt, TripleHashIndex}
import eu.easyminer.rdf.rule.ExtendedRule.{ClosedRule, DanglingRule}
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.IteratorExtensions._
import eu.easyminer.rdf.utils.Debugger
import eu.easyminer.rdf.utils.HowLong._

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleExpansion extends AtomCounting {

  implicit val debugger: Debugger
  val tripleIndex: TripleHashIndex
  val isWithInstances: Boolean
  val withDuplicitPredicates: Boolean
  val minHeadCoverage: Double
  val maxRuleLength: Int
  val bodyPattern: IndexedSeq[AtomPattern]

  implicit class PimpedRule(rule: ExtendedRule) {

    type Projections = collection.mutable.HashMap[Atom, IncrementalInt]
    type TripleProjections = collection.mutable.HashSet[Atom]

    /**
      * Next possible dangling variable for this rule
      */
    val dangling: Atom.Variable = rule.maxVariable.++

    /**
      * Patterns for remaining fresh atoms which can be added to this rule
      */
    private val patternAtom = bodyPattern.drop(rule.body.size).headOption

    /**
      * Map of all rule predicates. Each predicate has subject and object variables.
      * For each this variable in a particular position it has list of other items (in subject or object position).
      * List of other items may contain both variables and constants.
      * Ex: predicate 1 -> ( subject a -> List(object b, object B1, object B2) )
      */
    private val rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition, collection.Seq[Atom.Item]]] = {
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
      //if the predicate is not contained in the rule then !isCounted (COUNT IT!)
      rulePredicates.get(predicate).exists { predicate =>
        //if duplicit predicates are not allowed then then fresh atom is always counted and we dont count it!
        //we will find whether fresh atom should be counted, if yes then we return false (!isCounted - COUNT IT!)
        !withDuplicitPredicates || !((freshAtom.subject, freshAtom.`object`) match {
          //fresh atom is dangling and object is connector then we count this fresh atom only if:
          // - we want to create instantiated atom
          // - for this predicate there is an atom which has same object as this fresh atom
          // - for each these atoms all subject must be constants
          // - then we must count this atom; p(a, B) -> p(a, C) we count it; p(a, B) -> p(a, b) we dont count it (redundant noisy atom)
          //same case if subject is connector
          case (`dangling`, _) => instantiated && predicate.get(freshAtom.objectPosition).exists(_.forall(_.isInstanceOf[Atom.Constant]))
          case (_, `dangling`) => instantiated && predicate.get(freshAtom.subjectPosition).exists(_.forall(_.isInstanceOf[Atom.Constant]))
          //fresh atom is closed then we count this fresh atom only if:
          // - we dont want to create instantiated atom (instantiated atom is possible only for dangling fresh atom)
          // - for this predicate there is an atom which has same subject as this fresh atom
          // - for each these atoms all objets must not equal fresh atom object
          // - OR for this predicate there is an atom which has same object...same bahaviour
          //shortly, if fresh atom is closed and has shared item with other atom in the rule in the same position then we count it unless there is a duplicit atom
          case _ => !instantiated && (predicate.get(freshAtom.subjectPosition).exists(_.forall(_ != freshAtom.`object`)) || predicate.get(freshAtom.objectPosition).exists(_.forall(_ != freshAtom.subject)))
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
      * Check whether a new fresh atom is matching with the current atom pattern
      *
      * @param freshAtom new fresh atom
      * @return boolean
      */
    private def matchFreshAtom(freshAtom: FreshAtom) = patternAtom.forall { patternAtom =>
      (patternAtom.subject.isInstanceOf[Atom.Constant] || freshAtom.subject == patternAtom.subject) &&
        (patternAtom.`object`.isInstanceOf[Atom.Constant] || freshAtom.`object` == patternAtom.`object`)
    }

    /**
      * Check whether a new atom is matching with the current atom pattern
      *
      * @param atom new atom
      * @return boolean
      */
    private def matchAtom(atom: Atom) = patternAtom.forall { patternAtom =>
      patternAtom.predicate.forall(_ == atom.predicate) &&
        (atom.subject == patternAtom.subject || atom.subject.isInstanceOf[Atom.Constant] && patternAtom.subject.isInstanceOf[Atom.Variable]) &&
        (atom.`object` == patternAtom.`object` || atom.`object`.isInstanceOf[Atom.Constant] && patternAtom.`object`.isInstanceOf[Atom.Variable])
    }

    /**
      * From the current rule create new extended rules with all possible new atoms
      * New extended rules needn't be closed but contain maximal two dangling items.
      * One of two possible danglings is always within the last added atom.
      * Extended rules must fulfill all watching threshold conditions, constraints and patterns
      *
      * @return all extended rules with new atom
      */
    def expand: Iterator[ExtendedRule] = {
      implicit val projections: mutable.HashMap[Atom, IncrementalInt] = mutable.HashMap.empty
      //first we get all possible fresh atoms for the current rule (with one dangling variable or with two exising variables as closed rule)
      //we separate fresh atoms to two parts: countable and others (possible)
      //countable fresh atoms is atom for which we know all existing variables (we needn't know dangling variable)
      // - then we can count all projections for this fresh atom and then only check existence of rest atoms in the rule
      //for other fresh atoms we need to find instances for unknown variables (not for dangling variable)
      val (countableFreshAtoms, possibleFreshAtoms) = getPossibleFreshAtoms.filter(matchFreshAtom).toList.partition(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling))
      val minSupport = rule.headSize * minHeadCoverage
      val bodySet = rule.body.toSet
      //maxSupport is variable where the maximal support from all extension rules is saved
      var maxSupport = 0
      //this function creates variable map with specified head variables
      val specifyHeadVariableMapWithAtom: (Int, Int) => VariableMap = {
        val specifyVariableMapWithAtom = specifyVariableMapForAtom(rule.head)
        (s, o) => specifyVariableMapWithAtom(rule.head.copy(subject = Atom.Constant(s), `object` = Atom.Constant(o)), Map.empty)
      }
      debugger.debug("Rule expansion: " + rule, rule.headSize + 1) { ad =>
        logger.trace("Rule expansion - " + rule + "\n" + "countable: " + countableFreshAtoms + "\n" + "possible: " + possibleFreshAtoms)
        //if duplicit predicates are allowed then
        //for all fresh atoms return all extension with duplicit predicates by more efficient way
        howLong("Rule expansion - count duplicit", true) {
          if (withDuplicitPredicates) (countableFreshAtoms ::: possibleFreshAtoms).foreach(selectAtomsWithExistingPredicate)
        }
        ad.done()
        howLong("Rule expansion - count projections", true) {
          rule.headTriples.iterator.zipWithIndex.takeWhile { x =>
            //if max support + remaining steps is lower than min support we can finish "count projection" process
            //example 1: head size is 10, min support is 5. Only 4 steps are remaining and there are no projection found then we can stop "count projection"
            // - because no projection can have support greater or equal 5
            //example 2: head size is 10, min support is 5, remaining steps 2, projection with maximal support has value 2: 2 + 2 = 4 it is less than 5 - we can stop counting
            val remains = rule.headSize - x._2
            maxSupport + remains >= minSupport
          }.foreach { case ((_subject, _object), _) =>
            //for each triple covering head of this rule, find and count all possible projections for all possible fresh atoms
            val selectedAtoms = countProjections(bodySet, possibleFreshAtoms, countableFreshAtoms, specifyHeadVariableMapWithAtom(_subject, _object))
            for (atom <- selectedAtoms) {
              //for each found projection increase support by 1 and find max support
              maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
            }
            ad.done()
          }
        }
      }
      logger.trace("Rule expansion - total projections: " + projections.size)
      //filter all projections by minimal support and remove all duplicit projections
      //then create new rules from all projections (atoms)
      val ruleFilter = new MinSupportRuleFilter(minSupport) & new NoDuplicitRuleFilter(rule.head, bodySet) & new NoRepeatedGroups(withDuplicitPredicates, bodySet + rule.head, rulePredicates)
      projections.iterator.filter(x => ruleFilter(x._1, x._2.getValue)).map { case (atom, support) =>
        expandWithAtom(atom, support.getValue)
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
        val danglings = rule.variables.iterator.map(x => Iterator(FreshAtom(dangling, x), FreshAtom(x, dangling)))
        val closed = rule.variables.combinations(2).collect { case List(x, y) => Iterator(FreshAtom(x, y), FreshAtom(y, x)) }
        (danglings ++ closed).flatten
      case rule: DanglingRule =>
        rule.variables match {
          case ExtendedRule.OneDangling(dangling1, others) =>
            //if the rule has one dangling item then we create fresh atoms with this item and with a new dangling item
            // - result is one dangling rule again
            //or we create fresh atoms with existing dangling item with combination of other items
            // - result is closed rule
            val danglings = Iterator(FreshAtom(dangling1, dangling), FreshAtom(dangling, dangling1))
            val closed = others.iterator.flatMap(x => Iterator(FreshAtom(dangling1, x), FreshAtom(x, dangling1)))
            danglings ++ closed
          case ExtendedRule.TwoDanglings(dangling1, dangling2, _) =>
            //if the rule has two dangling items then we create fresh atoms with these items and with a new dangling item
            // - result is two danglings rule again
            //or we create fresh atoms only with these dangling items
            // - result is closed rule
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
      * @param freshAtom   any fresh atoms
      * @param projections map of projections which can be fulfilled if there is some duplicits within this new fresh atom
      */
    private def selectAtomsWithExistingPredicate(freshAtom: FreshAtom)
                                                (implicit projections: Projections = collection.mutable.HashMap.empty): Unit = {

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
        (Iterator(rule.head) ++ rule.body.iterator).filter(filterAtoms).distinctBy(_.predicate).foreach { atom =>
          //add this atom as a new projection with the same support as the current rule
          //if we want to create new atom with existing predicate then we may expand atom with variables only (not instantiated atom)
          // - only p(a, c) -> p(a, b) is allowed - not p(a, b) -> p(a, B), because it is noise = redundant pattern
          //if we want to create new instantiated atom with existing predicate then we may expand only other instantiated atom (not variable atom)
          // - only p(a, C) -> p(a, B) is allowed - not p(a, C) -> p(a, b), because it is noise = redundant pattern
          // - this phase (for instantioned atoms with existing predicates) is doing during the main count projections process
          if (atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
            projections += (Atom(freshAtom.subject, atom.predicate, freshAtom.`object`) -> IncrementalInt(rule.measures[Measure.Support].value))
          }
        }
        //we filter such all atoms that satisfy all constraints
        projections.retain((atom, _) => matchAtom(atom))
      }

      freshAtom match {
        case FreshAtom(`dangling`, variable) => countDanglingFreshAtom(TripleItemPosition.Object(variable))
        case FreshAtom(variable, `dangling`) => countDanglingFreshAtom(TripleItemPosition.Subject(variable))
        case _ =>
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
    private def countProjections(atoms: Set[Atom], possibleFreshAtoms: List[FreshAtom], countableFreshAtoms: List[FreshAtom], variableMap: VariableMap)
                                (implicit projections: TripleProjections = collection.mutable.HashSet.empty): TripleProjections = {
      //if there are some countable fresh atoms and there exists path for rest atoms then we can find all projections for these fresh atoms
      if (countableFreshAtoms.nonEmpty && exists(atoms, variableMap)) {
        countableFreshAtoms.foreach(countFreshAtom(_, variableMap))
      }
      //for each possible fresh we need to specify their variables
      if (possibleFreshAtoms.nonEmpty) {
        //select best atom and its score which has minimal projections from other atoms
        val (best, bestScore) = atoms.iterator.map(x => x -> scoreAtom(x, variableMap)).minBy(_._2)
        //select fresh atoms which have better score (lower) than best atom
        val (bestFreshAtoms, otherFreshAtoms) = possibleFreshAtoms.partition(scoreAtom(_, variableMap) <= bestScore)
        //for each this best fresh atom we directly specify all variables (enumerate all projections)
        //then for each projection we check existence of the rest of other atoms within this rule
        for (freshAtom <- bestFreshAtoms) {
          specifyAtom(freshAtom, variableMap) //specify fresh atom
            .filter(atom => !isCounted(freshAtom, atom.predicate)) //filter only atoms which need to be counted
            //for each predicate of the fresh atom we specify all variables and we filter projections which satisfy all constraints and are connected with the rest of other atoms
            .map(atom => atom.predicate -> specifyAtom(atom, variableMap).filter(atom => matchAtom(atom) && exists(atoms, variableMap + (freshAtom.subject -> atom.subject.asInstanceOf[Atom.Constant], freshAtom.`object` -> atom.`object`.asInstanceOf[Atom.Constant]))))
            .foreach { case (predicate, atoms) =>
              //check whether a predicate of new variable fresh atoms or instantiated fresh atoms has been counted in past (within duplicit predicates)
              //if yes we do not need to count projections for this predicate and fresh atom (for variable atoms or for instantiated atoms)
              val (notCountedInstanceProjections, notCountedVariableProjections) = (!isCounted(freshAtom, predicate, true), !isCounted(freshAtom, predicate, false))
              for (atom <- atoms) {
                if (isWithInstances && notCountedInstanceProjections) {
                  //if we may to create instantiated atoms for this predicate and fresh atom (it is allowed and not already counted)
                  //then we add new atom projection to atoms set
                  if (freshAtom.subject == dangling) projections += atom.copy(`object` = freshAtom.`object`)
                  else if (freshAtom.`object` == dangling) projections += atom.copy(subject = freshAtom.subject)
                }
                //if we may to create variable atoms for this predicate and fresh atom (not already counted)
                //then we add new atom projection to atoms set
                if (notCountedVariableProjections) projections += Atom(freshAtom.subject, atom.predicate, freshAtom.`object`)
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
            countProjections(rest, restAtoms, countableAtoms, variableMap)
          }
        }
      }
      projections
    }

    /**
      * if all variables are known (instead of dangling nodes) we can enumerate all projections
      *
      * @param atom        countable fresh atom
      * @param variableMap variable map to constants
      * @param projections projections repository
      */
    private def countFreshAtom(atom: FreshAtom, variableMap: VariableMap)
                              (implicit projections: TripleProjections): Unit = {
      //first we map all variables to constants
      (variableMap.getOrElse(atom.subject, atom.subject), variableMap.getOrElse(atom.`object`, atom.`object`)) match {
        //if there is variable on the subject place (it is dangling), count all dangling projections
        case (sv: Atom.Variable, Atom.Constant(oc)) =>
          //get all predicates for this object constant
          //skip all counted predicates that are included in this rule
          for ((predicate, subjects) <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates)) {
            //if there exists atom in rule which has same predicate and object variable and is not instationed
            // - then we dont use this fresh atom for this predicate because p(a, B) -> p(a, b) is not allowed (redundant and noisy rule)
            if (isWithInstances && !isCounted(atom, predicate, true)) {
              for (subject <- subjects; newAtom = Atom(Atom.Constant(subject), predicate, atom.`object`) if matchAtom(newAtom)) projections += newAtom
            }
            //we dont count fresh atom only with variables if there exists atom in rule which has same predicate and object variable
            // - because for p(a, c) -> p(a, b) it is counted AND for p(a, c) -> p(a, B) it is forbidden combination (redundant and noisy rule)
            if (!isCounted(atom, predicate, false)) {
              val newAtom = Atom(sv, predicate, atom.`object`)
              if (matchAtom(newAtom)) projections += newAtom
            }
          }
        //if there is variable on the object place (it is dangling), count all dangling projections
        case (Atom.Constant(sc), ov: Atom.Variable) =>
          //get all predicates for this subject constant
          //skip all counted predicates that are included in this rule
          for ((predicate, objects) <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates)) {
            if (isWithInstances && !isCounted(atom, predicate, true)) {
              for (_object <- objects; newAtom = Atom(atom.subject, predicate, Atom.Constant(_object)) if matchAtom(newAtom)) projections += newAtom
            }
            if (!isCounted(atom, predicate, false)) {
              val newAtom = Atom(atom.subject, predicate, ov)
              if (matchAtom(newAtom)) projections += newAtom
            }
          }
        //all variables are constants, there are not any dangling variables - atom is closed; there is only one projection
        case (Atom.Constant(sc), Atom.Constant(oc)) =>
          //we skip counted predicate because in this case we count only with closed atom which are not counted for the existing predicate
          //we need to count all closed atoms
          for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.objects.get(oc).iterator.flatten) if !isCounted(atom, predicate, false); newAtom = Atom(atom.subject, predicate, atom.`object`) if matchAtom(newAtom)) {
            projections += newAtom
          }
        case _ =>
      }
    }

    /**
      * Create new rule from this rule with added atom
      * It creates three variants: ClosedRule, OneDanglingRule, TwoDanglingsRule
      *
      * @param atom    new atom which will be added to this rule
      * @param support support of this rule with new atom
      * @return extended rule with new atom
      */
    private def expandWithAtom(atom: Atom, support: Int): ExtendedRule = {
      val measures = Measure.Measures(
        Measure.Support(support),
        Measure.HeadCoverage(support / rule.headSize.toDouble),
        Measure.HeadSize(rule.headSize)
      )
      (atom.subject, atom.`object`) match {
        case (sv: Atom.Variable, ov: Atom.Variable) => if (sv == dangling || ov == dangling) {
          rule match {
            case rule: DanglingRule => rule.variables match {
              case ExtendedRule.OneDangling(originalDangling, others) =>
                //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
                rule.copy(body = atom +: rule.body)(measures, ExtendedRule.OneDangling(dangling, originalDangling :: others), dangling, rule.headTriples)
              case ExtendedRule.TwoDanglings(dangling1, dangling2, others) =>
                //(d, c) | (a, c) (a, b) => TwoDanglings(c, b) -> TwoDanglings(d, b)
                val (pastDangling, secondDangling) = if (sv == dangling1 || ov == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
                rule.copy(body = atom +: rule.body)(measures, ExtendedRule.TwoDanglings(dangling, secondDangling, pastDangling :: others), dangling, rule.headTriples)
            }
            case rule: ClosedRule =>
              //(c, a) | (a, b) (a, b) => ClosedRule -> OneDangling(c)
              DanglingRule(atom +: rule.body, rule.head)(measures, ExtendedRule.OneDangling(dangling, rule.variables), dangling, rule.headTriples)
          }
        } else {
          rule match {
            case rule: ClosedRule =>
              //(a, b) | (a, b) (a, b) => ClosedRule -> ClosedRule
              rule.copy(atom +: rule.body)(measures, rule.variables, rule.maxVariable, rule.headTriples)
            case rule: DanglingRule =>
              //(c, a) | (c, a) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              //(c, b) |(a, c) (a, b) => TwoDanglings(c, b) -> ClosedRule
              ClosedRule(atom +: rule.body, rule.head)(measures, rule.variables.danglings ::: rule.variables.others, rule.maxVariable, rule.headTriples)
          }
        }
        case (_: Atom.Variable, _: Atom.Constant) | (_: Atom.Constant, _: Atom.Variable) => rule match {
          case rule: ClosedRule =>
            //(a, C) | (a, b) (a, b) => ClosedRule -> ClosedRule
            rule.copy(atom +: rule.body)(measures, rule.variables, rule.maxVariable, rule.headTriples)
          case rule: DanglingRule => rule.variables match {
            case ExtendedRule.OneDangling(dangling, others) =>
              //(c, C) | (a, c) (a, b) (a, b) => OneDangling(c) -> ClosedRule
              ClosedRule(atom +: rule.body, rule.head)(measures, dangling :: others, dangling, rule.headTriples)
            case ExtendedRule.TwoDanglings(dangling1, dangling2, others) =>
              //(c, C) | (a, c) (a, b) => TwoDanglings(c, b) -> OneDangling(b)
              val (pastDangling, dangling) = if (atom.subject == dangling1 || atom.`object` == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
              DanglingRule(atom +: rule.body, rule.head)(measures, ExtendedRule.OneDangling(dangling, pastDangling :: others), rule.maxVariable, rule.headTriples)
          }
        }
        case _ => throw new IllegalStateException
      }
    }

  }

}

object RuleExpansion {

  case class FreshAtom(subject: Atom.Variable, `object`: Atom.Variable) {
    def subjectPosition = TripleItemPosition.Subject(subject)

    def objectPosition = TripleItemPosition.Object(`object`)
  }

  implicit class PimpedAtom(atom: Atom) {
    def subjectPosition = TripleItemPosition.Subject(atom.subject)

    def objectPosition = TripleItemPosition.Object(atom.`object`)
  }

  sealed trait TripleItemPosition {
    val item: Atom.Item
  }

  object TripleItemPosition {

    case class Subject(item: Atom.Item) extends TripleItemPosition

    case class Object(item: Atom.Item) extends TripleItemPosition

  }

}