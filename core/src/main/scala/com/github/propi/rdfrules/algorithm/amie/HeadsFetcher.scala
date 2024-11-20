package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.rule.ExpandingRule.DanglingRule
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsForPredicates.ConstantsForPredicatePosition
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, MappedAtomPatternMatcher}
import com.github.propi.rdfrules.utils.Debugger.ActionDebugger
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

import java.util.concurrent.ForkJoinPool
import scala.collection.parallel.CollectionConverters.VectorIsParallelizable
import scala.collection.parallel.ForkJoinTaskSupport

/**
  * Created by Vaclav Zeman on 3. 6. 2019.
  */
trait HeadsFetcher extends AtomCounting {

  implicit val settings: AmieSettings
  implicit val forAtomMatcher: MappedAtomPatternMatcher[Atom]

  private def logicalHeadToRules(logicalHead: Atom)(implicit actionDebugger: ActionDebugger): Iterator[DanglingRule] = {
    val possibleAtoms = if (!settings.constantsPosition.contains(ConstantsPosition.Nowhere) || settings.constantsForPredicates.nonEmpty) {
      //if instantiated atoms are allowed then we specify variables by constants
      //only one variable may be replaced by a constant
      //result is original variable atom + instantied atoms with constants in subject + instantied atoms with constants in object
      //we do not instantient subject variable if onlyObjectInstances is true
      val resolvedConstantsPosition = settings.constantsForPredicates.get(logicalHead.predicate).map {
        case ConstantsForPredicatePosition.Subject => Some(ConstantsPosition.Subject)
        case ConstantsForPredicatePosition.Object => Some(ConstantsPosition.Object)
        case ConstantsForPredicatePosition.LowerCardinalitySide => Some(ConstantsPosition.LowerCardinalitySide(true))
        case ConstantsForPredicatePosition.Both => None
      }.getOrElse(settings.constantsPosition)
      val it = resolvedConstantsPosition match {
        case Some(ConstantsPosition.LowerCardinalitySide(_)) => tripleIndex.predicates(logicalHead.predicate).lowerCardinalitySide match {
          case TriplePosition.Subject => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0)))
          case TriplePosition.Object => specifyObject(logicalHead)
        }
        case Some(ConstantsPosition.Object) => specifyObject(logicalHead)
        case Some(ConstantsPosition.Subject) => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0)))
        case Some(ConstantsPosition.Nowhere) => Iterator.empty
        case _ => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0))) ++ specifyObject(logicalHead)
      }
      Iterator(logicalHead) ++ it.filter(settings.test)
    } else {
      Iterator(logicalHead)
    }
    //filter all atoms by patterns and join all valid patterns to the atom
    possibleAtoms.filter { atom =>
      settings.patterns.isEmpty || settings.patterns.exists(rp => rp.head.forall(atomPattern => forAtomMatcher.matchPattern(atom, atomPattern)(Aliases.empty).isDefined))
    }.flatMap { atom =>
      //convert all atoms to rules and filter it by min support
      val tm = tripleIndex.predicates(atom.predicate)
      actionDebugger.done()
      val danglingRule = Some(atom.subject, atom.`object`).collect {
        case (v1: Atom.Variable, v2: Atom.Variable) => (List(v2, v1), tm.size(settings.injectiveMapping), tm.size(settings.injectiveMapping))
        case (Atom.Constant(c), v1: Atom.Variable) => (List(v1), tm.size(settings.injectiveMapping), tm.subjects.get(c).map(_.size(settings.injectiveMapping)).getOrElse(0))
        case (v1: Atom.Variable, Atom.Constant(c)) => (List(v1), tm.size(settings.injectiveMapping), tm.objects.get(c).map(_.size(settings.injectiveMapping)).getOrElse(0))
      }.filter(x => x._2 >= settings.minHeadSize && x._3 >= math.max(settings.minSupport, settings.minHeadCoverage * x._2)).map(x =>
        DanglingRule(Vector.empty, atom,
          x._3,
          x._2,
          x._3,
          x._1,
          Nil
        )
      )
      danglingRule
    }
  }

  /**
    * Get all possible heads from triple index
    * All heads are filtered by constraints, patterns and minimal support
    *
    * @return atoms in dangling rule form - DanglingRule(no body, one head, one or two variables in head)
    */
  def getHeads(implicit debugger: Debugger): ForEach[ExpandingRule] = {
    //enumerate all possible head atoms with variables and instances
    //all unsatisfied predicates are filtered by constraints
    (f: ExpandingRule => Unit) => {
      val logicalHeads = tripleIndex.predicates.iterator.filter(settings.isValidPredicate).map(Atom(Atom.Variable(0), _, Atom.Variable(1))).filter(settings.test).toVector.par
      logicalHeads.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(settings.parallelism))
      debugger.debug("Heads mining") { implicit ad =>
        logicalHeads.foreach { head =>
          logicalHeadToRules(head).foreach(f)
        }
      }
    }
  }

}