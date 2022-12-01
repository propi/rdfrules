package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, InstantiatedAtom, Measure, ResolvedAtom, ResolvedRule, TripleItemPosition}
import com.github.propi.rdfrules.utils.{Debugger, IncrementalInt, TypedKeyMap}

import scala.collection.mutable

trait RuleEnhancement extends AtomCounting with RuleExpansion {
  val rule: ExpandingRule
  val settings: AmieSettings
  protected val debugger: Debugger

  import settings._

  /**
    * Next possible dangling variable for this rule
    */
  lazy val dangling: Atom.Variable = rule.maxVariable.++

  protected lazy val bodySet: Set[Atom] = rule.body.toSet

  protected lazy val minCurrentSupport: Double = minComputedSupport(rule)

  /**
    * Map of all rule predicates. Each predicate has subject and object variables.
    * For each this variable in a particular position it has list of other items (in subject or object position).
    * List of other items may contain both variables and constants.
    * Ex: "predicate 1" -> ( "subject variable a" -> List(object b, object B1, object B2) )
    */
  protected lazy val rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition[Atom.Item], collection.Seq[Atom.Item]]] = {
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
    * if minAtomSize is lower than 0 then the atom size must be greater than or equal to minCurrentSupport
    */
  protected lazy val testAtomSize: Option[Int => Boolean] = {
    if (minAtomSize == 0) {
      None
    } else if (minAtomSize < 0) {
      Some((atomSize: Int) => atomSize >= minCurrentSupport)
    } else {
      Some((atomSize: Int) => atomSize >= minAtomSize)
    }
  }

  protected def enhance[T](actionName: String, enhancingAtoms: Partitioned[T], ruleFilter: RuleFilter)(bindProjections: (Set[Atom], Partitioned.Part1[T], Partitioned.Part2[T], VariableMap) => mutable.HashSet[Atom]): Iterator[ExpandingRule] = {
    implicit val projections: mutable.HashMap[Atom, IncrementalInt] = mutable.HashMap.empty
    //maxSupport is variable where the maximal support from all extension rules is saved
    var maxSupport = 0
    //this function creates variable map with specified head variables
    val specifyHeadVariableMapWithAtom: (Int, Int) => VariableMap = {
      val specifyVariableMapWithAtom = specifyVariableMapForAtom(rule.head)
      (s, o) => specifyVariableMapWithAtom(InstantiatedAtom(s, rule.head.predicate, o), VariableMap(injectiveMapping))
    }
    val headSize = rule.headSize
    lazy val resolvedRule = ResolvedRule(rule.body.map(ResolvedAtom(_)), rule.head)(TypedKeyMap(Measure.HeadSize(rule.headSize), Measure.Support(rule.support), Measure.HeadCoverage(rule.headCoverage)))
    var lastDumpDuration = currentDuration
    rule.headTriples(injectiveMapping).zipWithIndex.takeWhile { x =>
      //if max support + remaining steps is lower than min support we can finish "count projection" process
      //example 1: head size is 10, min support is 5. Only 4 steps are remaining and there are no projection found then we can stop "count projection"
      // - because no projection can have support greater or equal 5
      //example 2: head size is 10, min support is 5, remaining steps 2, projection with maximal support has value 2: 2 + 2 = 4 it is less than 5 - we can stop counting
      val remains = headSize - x._2
      maxSupport + remains >= minCurrentSupport
    }.foreach { case ((_subject, _object), i) =>
      //for each triple covering head of this rule, find and count all possible projections for all possible fresh atoms
      val selectedAtoms = /*howLong("Rule expansion - bind projections", true)(*/ bindProjections(bodySet, enhancingAtoms.part1, enhancingAtoms.part2, specifyHeadVariableMapWithAtom(_subject, _object)) //)
      for (atom <- selectedAtoms) {
        //for each found projection increase support by 1 and find max support
        maxSupport = math.max(projections.getOrElseUpdate(atom, IncrementalInt()).++.getValue, maxSupport)
      }
      val miningDuration = currentDuration
      if (miningDuration - lastDumpDuration > 30000) {
        debugger.logger.info(s"Long $actionName of rule $resolvedRule. Projections size: ${projections.size}. Step: $i of $headSize")
        lastDumpDuration = miningDuration
        if (timeout.exists(miningDuration >= _) || debugger.isInterrupted) {
          maxSupport = Int.MinValue
          projections.clear()
        }
      }
    }
    projections.iterator.filter { case (atom, support) =>
      ruleFilter(atom, support.getValue)
    }.map { case (atom, support) =>
      expand(atom, support.getValue)
    }
  }

}

object RuleEnhancement {

  implicit class PimpedRule(extendedRule: ExpandingRule)(implicit ti: TripleIndex[Int], tii: TripleItemIndex, settings: AmieSettings, _debugger: Debugger) {
    def instantiate(atoms: Iterator[Atom]): Iterator[ExpandingRule] = {
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
      new RuleInstantiation {
        val rule: ExpandingRule = extendedRule
        val settings: AmieSettings = _settings
        val tripleIndex: TripleIndex[Int] = implicitly[TripleIndex[Int]]
        val tripleItemIndex: TripleItemIndex = implicitly[TripleItemIndex]
        protected val debugger: Debugger = _debugger
      }.instantiate(atoms)
      //}
    }

    def refine: Iterator[(ExpandingRule, Boolean)] = {
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