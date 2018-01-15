package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.ExtendedRule.{ClosedRule, DanglingRule}
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong._
import eu.easyminer.rdf.utils.{Debugger, HashQueue}

import scala.collection.parallel.immutable.ParVector

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePatterns: List[RulePattern], constraints: List[RuleConstraint])(implicit debugger: Debugger) {

  private val logger = Logger[Amie]

  lazy val minSupport: Int = thresholds[Threshold.MinSupport].value

  def addThreshold(threshold: Threshold): Amie = {
    thresholds += threshold
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint): Amie = new Amie(thresholds, rulePatterns, ruleConstraint :: constraints)

  def addRulePattern(rulePattern: RulePattern): Amie = new Amie(thresholds, rulePattern :: rulePatterns, constraints)

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(tripleIndex: TripleHashIndex): Traversable[ClosedRule] = {
    //create amie process with debugger and final triple index
    val process = new AmieProcess(tripleIndex)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger)
    //get all possible heads
    val heads = {
      val builder = ParVector.newBuilder[DanglingRule]
      builder ++= process.getHeads
      builder.result()
    }
    //parallel rule mining: for each head we search rules
    debugger.debug("Amie rules mining", heads.length) { ad =>
      heads.flatMap { head =>
        ad.result()(process.searchRules(head))
      }.seq
    }
  }

  private class AmieProcess(val tripleIndex: TripleHashIndex)(implicit val debugger: Debugger) extends RuleExpansion with AtomCounting {

    val isWithInstances: Boolean = constraints.exists(_.isInstanceOf[RuleConstraint.WithInstances])
    val minHeadCoverage: Double = thresholds[Threshold.MinHeadCoverage].value
    val maxRuleLength: Int = thresholds[Threshold.MaxRuleLength].value
    //val bodyPatterns: Seq[IndexedSeq[AtomPattern]] = rulePatterns.map(_.antecedent)
    val withDuplicitPredicates: Boolean = !constraints.contains(RuleConstraint.WithoutDuplicitPredicates)
    val onlyObjectInstances: Boolean = constraints.exists {
      case RuleConstraint.WithInstances(true) => true
      case _ => false
    }
    val atomMatcher: AtomPatternMatcher[Atom] = AtomPatternMatcher.ForAtom
    val freshAtomMatcher: AtomPatternMatcher[FreshAtom] = AtomPatternMatcher.ForFreshAtom

    private val (onlyPredicates, withoutPredicates) = {
      val a = constraints.collectFirst { case RuleConstraint.OnlyPredicates(p) => p }
      val b = constraints.collectFirst { case RuleConstraint.WithoutPredicates(p) => p }
      a -> b
    }

    def isValidPredicate(predicate: Int): Boolean = onlyPredicates.forall(_ (predicate)) && withoutPredicates.forall(!_ (predicate))

    /**
      * Get all possible heads from triple index
      * All heads are filtered by constraints, patterns and minimal support
      *
      * @return atoms in dangling rule form - DanglingRule(no body, one head, one or two variables in head)
      */
    def getHeads: Iterator[DanglingRule] = {
      //enumerate all possible head atoms with variables and instances
      //all unsatisfied predicates are filtered by constraints
      val possibleAtoms = {
        val it = tripleIndex.predicates.keysIterator.filter(isValidPredicate).map(Atom(Atom.Variable(0), _, Atom.Variable(1)))
        if (isWithInstances) {
          //if instantiated atoms are allowed then we specify variables by constants
          //only one variable may be replaced by a constant
          //result is original variable atom + instantied atoms with constants in subject + instantied atoms with constants in object
          //we do not instantient subject variable if onlyObjectInstances is true
          it.flatMap { atom =>
            val it1 = if (onlyObjectInstances) {
              Iterator.empty
            } else {
              tripleIndex.predicates(atom.predicate).subjects.keysIterator.map(subject => Atom(Atom.Constant(subject), atom.predicate, Atom.Variable(0)))
            }
            val it2 = tripleIndex.predicates(atom.predicate).objects.keysIterator.map(`object` => Atom(Atom.Variable(0), atom.predicate, Atom.Constant(`object`)))
            Iterator(atom) ++ it1 ++ it2
          }
        } else {
          it
        }
      }
      //filter all atoms by patterns and join all valid patterns to the atom
      possibleAtoms.flatMap { atom =>
        val validPatterns = rulePatterns.filter(rp => rp.consequent.forall(atomPattern => atomMatcher.matchPattern(atom, atomPattern)))
        if (rulePatterns.isEmpty || validPatterns.nonEmpty) {
          Iterator(atom -> validPatterns)
        } else {
          Iterator.empty
        }
      }.flatMap { case (atom, patterns) =>
        //convert all atoms to rules and filter it by min support
        val tm = tripleIndex.predicates(atom.predicate)
        Some(atom.subject, atom.`object`).collect {
          case (v1: Atom.Variable, v2: Atom.Variable) => ExtendedRule.TwoDanglings(v1, v2, Nil) -> tm.size
          case (Atom.Constant(c), v1: Atom.Variable) => ExtendedRule.OneDangling(v1, Nil) -> tm.subjects.get(c).map(_.size).getOrElse(0)
          case (v1: Atom.Variable, Atom.Constant(c)) => ExtendedRule.OneDangling(v1, Nil) -> tm.objects.get(c).map(_.size).getOrElse(0)
        }.filter(_._2 >= minSupport).map(x =>
          DanglingRule(Vector.empty, atom)(
            Measure.Measures(Measure.HeadSize(x._2), Measure.Support(x._2)),
            patterns,
            x._1,
            x._1.danglings.max,
            getAtomTriples(atom).toIndexedSeq
          )
        )
      }
    }

    /**
      * Search all expanded rules from a specific rule which satisfies all thresholds, constraints and patterns.
      *
      * @param initRule a rule to be expanded
      * @return expanded closed rules
      */
    def searchRules(initRule: ExtendedRule): List[ClosedRule] = {
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      val queue = new HashQueue[ExtendedRule].add(initRule)
      //list for mined closed rules
      val result = collection.mutable.ListBuffer.empty[ClosedRule]
      //if the queue is not empty, try expand rules
      while (!queue.isEmpty) {
        //get one rule from queue and remove it from that
        val rule = queue.poll
        //if the rule is closed we add it to the result set
        rule match {
          case closedRule: ClosedRule => result += closedRule
          case _ =>
        }
        //if rule length is lower than max rule length we can expand this rule with one atom
        if (rule.ruleLength < maxRuleLength) {
          //expand the rule and add all expanded variants into the queue
          howLong("Rule expansion", true)(for (rule <- rule.expand) queue.add(rule))
        }
      }
      result.toList
    }

  }

}

object Amie {

  def apply()(implicit debugger: Debugger): Amie = {
    val defaultThresholds = Threshold.Thresholds(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, Nil, Nil)
  }

}