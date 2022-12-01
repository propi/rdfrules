package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.RuleFilter.{MinSupportRuleFilter, RuleConstraints, RulePatternFilter}
import com.github.propi.rdfrules.rule._

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleRefinement extends RuleEnhancement with FreshAtomGenerator {

  import settings._

  /**
    * From the current rule create new extended rules with all possible new atoms
    * New extended rules needn't be closed but contain maximal two dangling items.
    * One of two possible danglings is always within the last added atom.
    * Extended rules must fulfill all watching threshold conditions, constraints and patterns
    *
    * @return all extended rules with new atom
    */
  def refine: Iterator[(ExpandingRule, Boolean)] = {
    val patternFilter = new RulePatternFilter(rule, patterns, maxRuleLength, true)
    if (patterns.nonEmpty && !patternFilter.isDefined) {
      //if all paterns must be exact and are completely matched then we will not expand this rule
      Iterator.empty
    } else {
      enhance(
        "refining",
        Partitioned.from(getPossibleFreshAtoms.filter(patternFilter.matchFreshAtom))(freshAtom => List(freshAtom.subject, freshAtom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling)),
        new MinSupportRuleFilter(minCurrentSupport) & patternFilter & new RuleConstraints(rule, filters)
      )(bindProjections).map { expanded =>
        val isDangling = expanded.body.head.subject == dangling || expanded.body.head.`object` == dangling
        expanded -> isDangling
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
  private def bindProjections(atoms: Set[Atom], countableFreshAtoms: Partitioned.Part1[FreshAtom], possibleFreshAtoms: Partitioned.Part2[FreshAtom], variableMap: VariableMap)
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
        //for each predicate of the fresh atom we specify all variables and we filter projections which are connected with the rest of other atoms
        atom <- specifyAtom(atomWithSpecifiedPredicate, variableMap) if exists(atoms, variableMap + (freshAtom.subject -> Atom.Constant(atom.subject), atom.predicate, freshAtom.`object` -> Atom.Constant(atom.`object`)))
      } {
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
        val (countableAtoms, restAtoms) = otherFreshAtoms.partition(freshAtom => Iterator(freshAtom.subject, freshAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x)))
        for (variableMap <- specifyVariableMap(best, variableMap)) {
          //we specify best atom and specified variables (projection) send to the next iteration
          bindProjections(rest, countableAtoms, restAtoms, variableMap)
        }
      }
    }
    projections
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
      case (sv: Atom.Variable, Atom.Constant(oc)) =>
        //get all predicates for this object constant
        //skip all counted predicates that are included in this rule
        for (predicate <- tripleIndex.objects.get(oc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
          //if there exists atom in rule which has same predicate and object variable and is not instationed
          // - then we dont use this fresh atom for this predicate because p(a, B) -> p(a, b) is not allowed for functions (redundant and noisy rule)
          //we dont count fresh atom only with variables if there exists atom in rule which has same predicate and object variable
          // - because for p(a, c) -> p(a, b) it is counted AND for p(a, c) -> p(a, B) it is forbidden combination for functions (redundant and noisy rule)
          projections += Atom(sv, predicate, atom.`object`)
        }
      //if there is variable on the object place (it is dangling), count all dangling projections
      case (Atom.Constant(sc), ov: Atom.Variable) =>
        //get all predicates for this subject constant
        //skip all counted predicates that are included in this rule
        for (predicate <- tripleIndex.subjects.get(sc).iterator.flatMap(_.predicates.iterator) if isValidFreshPredicate(atom, predicate)) {
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