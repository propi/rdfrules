package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.ExpandingRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, FreshAtom}

trait FreshAtomGenerator {

  self: RuleRefinement =>

  import Atom.variableOrdering.mkOrderingOps
  import settings._

  //private val tupleVariableOrdering = implicitly[Ordering[(Atom.Variable, Atom.Variable)]]

  /* protected lazy val danglings: Set[Atom.Variable] = rule match {
     case x: ExpandingRule.DanglingRule => x.variables.danglings.toSet
     case _ => Set.empty
   }*/

  /*protected lazy val variablePairs: collection.Map[Atom.Variable, collection.Set[(Int, TripleItemPosition[Atom.Item])]] = {
    val hmap = collection.mutable.HashMap.empty[Atom.Variable, collection.mutable.Set[(Int, TripleItemPosition[Atom.Item])]]
    for (atom <- Iterator(rule.head) ++ rule.body.iterator) {
      atom.subject match {
        case x: Atom.Variable if danglings(x) =>
          hmap.getOrElseUpdate(x, collection.mutable.HashSet.empty).add(atom.predicate -> TripleItemPosition.Object(atom.`object`))
        case _ =>
      }
      atom.`object` match {
        case x: Atom.Variable if danglings(x) =>
          hmap.getOrElseUpdate(x, collection.mutable.HashSet.empty).add(atom.predicate -> TripleItemPosition.Subject(atom.subject))
        case _ =>
      }
    }
    hmap
  }*/

  /**
    * This is an auxiliary hmap used to prevent the generation of duplicate rules
    * Set(a, b) -> max(p) : set of variables pointing to max predicate.
    * E.g.: (b p1 a) => (a p b), hmap = Set(a, b) -> p1 if p1 >= p
    */
  private lazy val maxPredicates: collection.Map[Set[Atom.Variable], Int] = {
    val hmap = collection.mutable.HashMap.empty[Set[Atom.Variable], Int]
    for (atom <- rule.body) {
      val key = Iterator(atom.subject, atom.`object`).collect {
        case x: Atom.Variable => x
      }.toSet
      hmap.get(key) match {
        case Some(i) if i >= atom.predicate =>
        case _ => hmap.put(key, atom.predicate)
      }
    }
    hmap
  }

  /*private lazy val maxVariablePair = rule.body.iterator.map { atom =>
    atomItemToVariable(atom.subject) -> atomItemToVariable(atom.`object`)
  }.maxOption

  protected lazy val maxPredicates2: collection.Map[Atom.Variable, Int] = {
    val hmap = collection.mutable.HashMap.empty[Atom.Variable, Int]
    for (atom <- rule.body) {
      Iterator(atom.subject, atom.`object`).map {
        case x: Atom.Variable => x
        case _: Atom.Constant => dangling
      }.foreach(x => hmap.get(x) match {
        case Some(i) if i >= atom.predicate =>
        case _ => hmap.put(x, atom.predicate)
      })
    }
    hmap
  }*/

  /**
    * This is an auxiliary hset used to prevent the generation of duplicate rules
    * The collection set represents all variables in the rule which has an constant in a pair within an atom in the rule.
    * E.g. (a p C) => (a p b) : hset = Set(a) - a has constant C in atom (a p C)
    * For this rule we can not generate (a p c) & (a p C) => (a p b). First we need (a p c) then we can attach other atom with constant (a p C).
    */
  /*protected lazy val variablesWithConstants: collection.Set[Atom.Variable] = {
    val hset = collection.mutable.HashSet.empty[Atom.Variable]
    for (atom <- rule.body) {
      (atom.subject, atom.`object`) match {
        case (x: Atom.Variable, _: Atom.Constant) => hset.add(x)
        case (_: Atom.Constant, x: Atom.Variable) => hset.add(x)
        case _ =>
      }
    }
    hset
  }*/

  /**
    * Check whether the new fresh predicate is unique in the rule
    *
    * @param predicate fresh predicate
    * @return true - the predicate does not exist in the rule
    */
  private def isUniquePredicate(predicate: Int): Boolean = !rulePredicates.contains(predicate)

  /**
    * Check atom duplicity in the rule
    *
    * @param freshAtom fresh atom
    * @param predicate bound predicate
    * @return true if the fresh atom already exists in the rule
    */
  private def isDuplicateAtom(freshAtom: FreshAtom, predicate: Int): Boolean = {
    rulePredicates.get(predicate).exists(_.get(freshAtom.subjectPosition).exists(_.exists(_ == freshAtom.`object`)))
  }

  /**
    * If we generate a particular predicate for a fresh atom, we need to check the new atom whether it is valid or not
    *
    * @param freshAtom fresh atom
    * @param predicate bound predicate
    * @return true if it is valid
    */
  protected def isValidFreshPredicate(freshAtom: FreshAtom, predicate: Int): Boolean = {
    lazy val predicateIndex = tripleIndex.predicates(predicate)
    //first we check whether the predicate is in the list of enabled predicates
    isValidPredicate(predicate) &&
      //predicates should be generated ordered. If fresh variables (a c) already exists in the rule, e.g., (a p c) => (a p b)
      //then new predicate must be greater than or equal to p (this prevents duplicate generated rules)
      maxPredicates.get(freshAtom.variables).forall(predicate >= _) &&
      //we allow such predicates which reach a minimum atom size threshold
      testAtomSize.forall(_ (predicateIndex.size(injectiveMapping))) &&
      //we disable duplicate atoms in the rule or duplicate predicates if they are forbidden.
      (if (withDuplicitPredicates) !isDuplicateAtom(freshAtom, predicate) else isUniquePredicate(predicate))
  }

  /*lazy val equivalentAtoms = freshAtom match {
  case FreshAtom(s, o) if s == dangling && !danglings(o) => danglings.iterator.map(x => Set(o, x))
  case FreshAtom(s, o) if o == dangling && !danglings(s) => danglings.iterator.map(x => Set(s, x))
  case _ => Iterator.empty
}*/
  //if we have rule (c p a) => (a p b), then the fresh atom can not be (a p c). First we need (a p c) then (c p a) (this prevents duplicate generated rules)
  //!(freshAtom.subject < freshAtom.`object` && rulePredicates.get(predicate).exists(_.get(TripleItemPosition.Subject(freshAtom.`object`)).exists(_.exists(freshAtom.subject == _)))) &&
  //(!experiment || equivalentAtoms.forall(key => maxPredicates.get(key).forall(p => predicate > p || (predicate == p && freshAtom.`object` == dangling)))) &&
  //(!experiment || danglings.size != 2 || !danglings(freshAtom.subject) || !danglings(freshAtom.`object`) || variablePairs(freshAtom.subject) != variablePairs(freshAtom.`object`) || freshAtom.subject < freshAtom.`object`) &&
  //(!experiment || Iterator(freshAtom.subject, freshAtom.`object`).forall(x => maxPredicates2.get(x).forall(p => predicate > p || (predicate == p && maxVariablePair.forall(x => tupleVariableOrdering.gteq(freshAtom.subject -> freshAtom.`object`, x)))))) &&

  /**
    * Create all possible ordered combinations of fresh atoms which are connected to other rule atoms
    * (a b) < (a c) < (b c). It means if last atom of refining rule is (b c) then we can not generate (a b), (a c)
    *
    * @return fresh atoms iterator
    */
  protected def getPossibleFreshAtoms: Iterator[FreshAtom] = {
    //all fresh atoms must be greater then last added atom (e.g. (b, c) > (a, b))
    val head = if (rule.body.isEmpty) rule.head else rule.body.head
    //fresh atom variables are sorted first "a" then "b"
    //all constants are substituted with dangling variable, e.g., (a p C) => (a p b), then C = c
    val (x, y) = (head.subject, head.`object`) match {
      case (v1: Atom.Variable, v2: Atom.Variable) => v1.min(v2) -> v1.max(v2)
      case (v1: Atom.Variable, _) => v1 -> dangling
      case (_, v2: Atom.Variable) => v2 -> dangling
      case _ => throw new IllegalStateException()
    }
    val nextDangling = dangling.++
    val checkRightDanglings = rule match {
      //if closed rule any fresh atom is allowed
      case _: ClosedRule => (_: FreshAtom) => true
      case rule: DanglingRule => rule.variables match {
        //if rule has one dangling, e.g., => (a C), then new fresh atom (b c) should not be allowed because we can not close this rule
        //for example: (b c) => (a C) then we can not generate closing fresh atom (a *) due to fresh atoms ordering restriction
        //therefore any fresh variable should be lower than or equal to both rule dangling (b <= a) || (c <= a) - this condition is not met.
        case ExpandingRule.OneDangling(dangling1, _) => (x: FreshAtom) => x.subject <= dangling1 || x.`object` <= dangling1
        //1. if rule has two danglings, e.g., => (a b), then new fresh atom (b c) should not be allowed because we can not close this rule
        //for example: (b c) => (a b) then we can not generate closing fresh atom (a *) due to fresh atoms ordering restriction
        //therefore any fresh variable should be lower than or equal to both rule danglings (b <= a && b <= b) || (c <= a && c <= b) - this condition is not met.
        //2. we disable three danglings, max two are possible: (a c) => (a b), (a d) ^ (a c) => (a b) this is disabled
        //fresh variable must be dangling (c, b) or no fresh variable must be new dangling (d)
        case ExpandingRule.TwoDanglings(dangling1, dangling2, _) => (x: FreshAtom) =>
          (x.subject <= dangling1 && x.subject <= dangling2 || x.`object` <= dangling1 && x.`object` <= dangling2) &&
            (x.subject == dangling1 ||
              x.subject == dangling2 ||
              x.`object` == dangling1 ||
              x.`object` == dangling2 ||
              (x.subject != dangling && x.`object` != dangling))
      }
    }
    val checkLastAtom = if (rule.ruleLength + 1 < maxRuleLength) {
      (_: FreshAtom) => true
    } else {
      //if last atom is generating then some combinations should be disallowed.
      val danglingPos1 = constantsPosition match {
        //if constants possition is only subject, then new dangling for last atom can not be at the object position (must be subject)
        case Some(ConstantsPosition.Subject) => (x: FreshAtom) => x.`object` != dangling
        //if constants possition is only object, then new dangling for last atom can not be at the subject position (must be object)
        case Some(ConstantsPosition.Object) => (x: FreshAtom) => x.subject != dangling
        case _ => (_: FreshAtom) => true
      }
      val danglingPos2 = rule match {
        //if rule is closed and we mine with constants then any fresh atom is allowed because we will instantiate any fresh dangling variable
        case _: ClosedRule if isWithInstances => (_: FreshAtom) => true
        //if rule is closed and mining with constants is disabled then last fresh atom can not be opened (with dangling variable)
        case _: ClosedRule => (x: FreshAtom) => x.subject != dangling && x.`object` != dangling
        case rule: DanglingRule => rule.variables match {
          //if rule has one dangling and we mine with constants then the rule dangling must be closed within the last atom
          //shortly, we can not generate two danglings rule, e.g., (a C) => (a b) then this fresh atom is disabled (a, c)
          case ExpandingRule.OneDangling(dangling1, _) if isWithInstances => (x: FreshAtom) =>
            x.subject == dangling1 ||
              x.`object` == dangling1
          //one dangling rule without constants must be closed within last atom
          case ExpandingRule.OneDangling(dangling1, _) => (x: FreshAtom) =>
            (x.subject == dangling1 && x.`object` != dangling) ||
              (x.`object` == dangling1 && x.subject != dangling)
          //for two danglingss last atom must close both danglings
          case ExpandingRule.TwoDanglings(dangling1, dangling2, _) => (x: FreshAtom) =>
            (x.subject == dangling1 && x.`object` == dangling2) || (x.subject == dangling2 && x.`object` == dangling1)
        }
      }
      (x: FreshAtom) => danglingPos1(x) && danglingPos2(x)
    }

    val nextX = x.++
    //fresh variables are generating with strict ordering
    //(a b) < (a c) < (b c). It means if last atom of refining rule is (b c) then we can not generate (a b), (a c)
    (for {
      i <- Iterator.iterate(x)(_.++).takeWhile(_ != dangling)
      j <- Iterator.iterate(nextX)(_.++).takeWhile(_ != nextDangling) if i < j && ((i == x && j >= y) || i > x)
    } yield {
      //after generate the ordered atom variables, we constuct fresh atom and check other conditions.
      Iterator(FreshAtom(i, j), FreshAtom(j, i)).filter(x => checkRightDanglings(x) && checkLastAtom(x))
    }).flatten
  }

}
