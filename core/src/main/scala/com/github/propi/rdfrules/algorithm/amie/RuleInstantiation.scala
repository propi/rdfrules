package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.RuleFilter.{MinSupportRuleFilter, QuasiBindingFilter, RuleConstraints, RulePatternFilter}
import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 6. 2017.
  */
trait RuleInstantiation extends RuleEnhancement {

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
    *
    * @param predicate predicate
    * @return
    */
  private def instantiatedPosition(predicate: Int): Option[ConceptPosition] = constantsPosition.map {
    case ConstantsPosition.Object => TriplePosition.Object
    case ConstantsPosition.LowerCardinalitySide => tripleIndex.predicates(predicate).lowerCardinalitySide
    case ConstantsPosition.Subject => TriplePosition.Subject
    case _ => TriplePosition.Object
  }

  def instantiate(atoms: Iterator[Atom]): Iterator[ExpandingRule] = {
    val patternFilter = new RulePatternFilter(rule, patterns, maxRuleLength, false)
    if (patterns.nonEmpty && !patternFilter.isDefined) {
      //if all paterns must be exact and are completely matched then we will not expand this rule
      Iterator.empty
    } else {
      enhance(
        "instantiating",
        Partitioned.from(atoms.filter { atom =>
          val danglingPosition = if (atom.subject == dangling) TriplePosition.Subject else TriplePosition.Object
          instantiatedPosition(atom.predicate).forall(_ == danglingPosition)
        })(atom => List(atom.subject, atom.`object`).forall(x => x == rule.head.subject || x == rule.head.`object` || x == dangling)),
        new MinSupportRuleFilter(minCurrentSupport) & patternFilter & new RuleConstraints(rule, filters) & new QuasiBindingFilter(bodySet, injectiveMapping, this)
      )(bindProjections)
    }
  }

  private def bindProjections(atoms: Set[Atom], countableInstantiatingAtoms: Partitioned.Part1[Atom], possibleInstantiatingAtoms: Partitioned.Part2[Atom], variableMap: VariableMap)
                             (implicit projections: mutable.HashSet[Atom] = mutable.HashSet.empty): mutable.HashSet[Atom] = {
    if (countableInstantiatingAtoms.nonEmpty && exists(atoms, variableMap)) {
      countableInstantiatingAtoms.foreach(bindAtom(_, atoms, variableMap))
    }
    if (possibleInstantiatingAtoms.nonEmpty) {
      val (best, bestScore) = atoms.iterator.map(x => x -> scoreAtom(x, variableMap)).minBy(_._2)
      val (bestInstantiatingAtoms, otherInstantiatingAtoms) = possibleInstantiatingAtoms.partition(scoreAtom(_, variableMap) <= bestScore)
      bestInstantiatingAtoms.foreach { instantiatingAtom =>
        val maxConstant = if (instantiatingAtom.subject == dangling) {
          maxConstants.get(instantiatingAtom.objectPosition.asInstanceOf[TripleItemPosition[Atom.Variable]] -> instantiatingAtom.predicate)
        } else {
          maxConstants.get(instantiatingAtom.subjectPosition.asInstanceOf[TripleItemPosition[Atom.Variable]] -> instantiatingAtom.predicate)
        }
        for (instantiatedAtom <- specifyAtom(instantiatingAtom, variableMap) if exists(atoms, variableMap + (instantiatingAtom.subject.asInstanceOf[Atom.Variable] -> Atom.Constant(instantiatedAtom.subject), instantiatedAtom.predicate, instantiatingAtom.`object`.asInstanceOf[Atom.Variable] -> Atom.Constant(instantiatedAtom.`object`)))) {
          if (instantiatingAtom.subject == dangling && maxConstant.forall(instantiatedAtom.subject > _) && testAtomSize.forall(_ (tripleIndex.predicates(instantiatedAtom.predicate).subjects(instantiatedAtom.subject).size(injectiveMapping)))) projections += Atom(Atom.Constant(instantiatedAtom.subject), instantiatedAtom.predicate, instantiatingAtom.`object`)
          else if (instantiatingAtom.`object` == dangling && maxConstant.forall(instantiatedAtom.subject > _) && testAtomSize.forall(_ (tripleIndex.predicates(instantiatedAtom.predicate).objects(instantiatedAtom.`object`).size(injectiveMapping)))) projections += Atom(instantiatingAtom.subject, instantiatedAtom.predicate, Atom.Constant(instantiatedAtom.subject))
        }
      }
      if (otherInstantiatingAtoms.nonEmpty) {
        val rest = if (atoms.size == 1) Set.empty[Atom] else atoms - best
        val (countableAtoms, restAtoms) = otherInstantiatingAtoms.partition(instantiatingAtom => Iterator(instantiatingAtom.subject, instantiatingAtom.`object`).forall(x => x == dangling || x == best.subject || x == best.`object` || variableMap.contains(x.asInstanceOf[Atom.Variable])))
        for (variableMap <- specifyVariableMap(best, variableMap)) {
          bindProjections(rest, countableAtoms, restAtoms, variableMap)
        }
      }
    }
    projections
  }

  private def isDuplicateInstantiatedAtom(p: Int, v: TripleItemPosition[Atom.Item], rest: Set[Atom], variableMap: VariableMap): (Atom.Constant, Atom.Constant) => Boolean = {
    if (withDuplicitPredicates) {
      val items = rulePredicates.get(p).flatMap(_.get(v)).getOrElse(Nil)
      val f = (s: Atom.Constant, o: Atom.Constant, x: Atom.Constant) => (injectiveMapping && s == o) || items.exists {
        //(?a p C) ^ (?a p C) => (?a p1 X) - DUPLICATE!
        case Atom.Constant(c) => x.value == c
        case y: Atom.Variable if variableMap.injectiveMapping =>
          variableMap.get(y) match {
            //(?a p C) ^ (?a p ?b) => (?a p1 ?b) - VariableMap(?b -> C) - DUPLICATE!
            case Some(Atom.Constant(c)) => x.value == c
            //(?a p C) ^ (?a p ?b) => (?a p1 ?b) - VariableMap(?b -> NONE) - EXISTS(VariableMap(?b -> C)) - DUPLICATE!
            case None => !exists(rest, variableMap + (s, p, o)) //IF NOT exists any other binding, e.g., (?a p C) ^ (?a p D), THEN it is DUPLICATE!
          }
        case _ => false
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

  /**
    * if all variables are known (instead of dangling nodes) we can enumerate all projections
    *
    * @param atom        countable fresh atom
    * @param variableMap variable map to constants
    * @param projections projections repository
    */
  private def bindAtom(atom: Atom, rest: Set[Atom], variableMap: VariableMap)
                      (implicit projections: mutable.HashSet[Atom]): Unit = {
    (variableMap.specifyItem(atom.subject), variableMap.specifyItem(atom.`object`)) match {
      case (_: Atom.Variable, o@Atom.Constant(oc)) =>
        for (subjects <- tripleIndex.predicates(atom.predicate).objects.get(oc)) {
          val isDup = isDuplicateInstantiatedAtom(atom.predicate, atom.objectPosition, rest, variableMap)
          val maxConstant = maxConstants.get(atom.objectPosition.asInstanceOf[TripleItemPosition[Atom.Variable]] -> atom.predicate)
          for (subject <- subjects.iterator.map(Atom.Constant) if !isDup(subject, o) && maxConstant.forall(subject.value > _) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).subjects(subject.value).size(injectiveMapping)))) projections += Atom(subject, atom.predicate, atom.`object`)
        }
      case (s@Atom.Constant(sc), _: Atom.Variable) =>
        for (objects <- tripleIndex.predicates(atom.predicate).subjects.get(sc)) {
          val isDup = isDuplicateInstantiatedAtom(atom.predicate, atom.subjectPosition, rest, variableMap)
          val maxConstant = maxConstants.get(atom.subjectPosition.asInstanceOf[TripleItemPosition[Atom.Variable]] -> atom.predicate)
          for (_object <- objects.iterator.map(Atom.Constant) if !isDup(s, _object) && maxConstant.forall(_object.value > _) && testAtomSize.forall(_ (tripleIndex.predicates(atom.predicate).objects(_object.value).size(injectiveMapping)))) projections += Atom(atom.subject, atom.predicate, _object)
        }
      case _ =>
    }
  }

}