package eu.easyminer.rdf.algorithm.amie

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.algorithm.RulesMining
import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.ExtendedRule.{ClosedRule, DanglingRule}
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.HowLong._
import eu.easyminer.rdf.utils.{Debugger, HashQueue, TypedKeyMap}

import scala.collection.parallel.immutable.ParVector

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(implicit debugger: Debugger) extends RulesMining {

  private val logger = Logger[Amie]

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(tripleIndex: TripleHashIndex): IndexedSeq[ClosedRule] = {
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

    private var _minHeadCoverage = thresholds.apply[Threshold.MinHeadCoverage].value

    def minHeadCoverage: Double = _minHeadCoverage

    val minSupport: Int = thresholds.apply[Threshold.MinSupport].value
    val topK: Int = thresholds.get[Threshold.TopK].map(_.value).getOrElse(0)
    val isWithInstances: Boolean = constraints.exists[RuleConstraint.WithInstances]
    val maxRuleLength: Int = thresholds.apply[Threshold.MaxRuleLength].value
    val withDuplicitPredicates: Boolean = constraints.exists[RuleConstraint.WithoutDuplicitPredicates]
    val onlyObjectInstances: Boolean = constraints.get[RuleConstraint.WithInstances].exists(_.onlyObjects)
    val atomMatcher: AtomPatternMatcher[Atom] = AtomPatternMatcher.ForAtom
    val freshAtomMatcher: AtomPatternMatcher[FreshAtom] = AtomPatternMatcher.ForFreshAtom

    private val onlyPredicates = constraints.get[RuleConstraint.OnlyPredicates].map(_.predicates)
    private val withoutPredicates = constraints.get[RuleConstraint.WithoutPredicates].map(_.predicates)

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
        val validPatterns = patterns.filter(rp => rp.consequent.forall(atomPattern => atomMatcher.matchPattern(atom, atomPattern)))
        if (patterns.isEmpty || validPatterns.nonEmpty) {
          Iterator(atom -> validPatterns)
        } else {
          Iterator.empty
        }
      }.flatMap { case (atom, patterns) =>
        //convert all atoms to rules and filter it by min support
        val tm = tripleIndex.predicates(atom.predicate)
        val danglingRule = Some(atom.subject, atom.`object`).collect {
          case (v1: Atom.Variable, v2: Atom.Variable) => ExtendedRule.TwoDanglings(v1, v2, Nil) -> tm.size
          case (Atom.Constant(c), v1: Atom.Variable) => ExtendedRule.OneDangling(v1, Nil) -> tm.subjects.get(c).map(_.size).getOrElse(0)
          case (v1: Atom.Variable, Atom.Constant(c)) => ExtendedRule.OneDangling(v1, Nil) -> tm.objects.get(c).map(_.size).getOrElse(0)
        }.filter(_._2 >= minSupport).map(x =>
          DanglingRule(Vector.empty, atom)(
            TypedKeyMap(Measure.HeadSize(x._2), Measure.Support(x._2), Measure.HeadCoverage(1.0)),
            patterns,
            x._1,
            x._1.danglings.max,
            getAtomTriples(atom).toIndexedSeq
          )
        )
        danglingRule
      }
    }

    /**
      * Search all expanded rules from a specific rule which satisfies all thresholds, constraints and patterns.
      *
      * @param initRule a rule to be expanded
      * @return expanded closed rules
      */
    def searchRules(initRule: ExtendedRule): Seq[ClosedRule] = {
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      val queue = new HashQueue[ExtendedRule].add(initRule)
      //collection for mined closed rules
      //there are two collections for two mining approaches: byMinSupport or topK
      //for topK we need to use priority queue where on the peek there is rule with lowest HC
      //for byMinSupport we need only a simple buffer
      val result = if (topK > 0) {
        Left(collection.mutable.PriorityQueue.empty[ClosedRule](Ordering.by[ClosedRule, Double](_.measures[Measure.HeadCoverage].value).reverse))
      } else {
        Right(collection.mutable.ListBuffer.empty[ClosedRule])
      }
      //if the queue is not empty, try expand rules
      while (!queue.isEmpty) {
        //get one rule from queue and remove it from that
        val rule = queue.poll
        //if the rule is closed we add it to the result set
        rule match {
          case closedRule: ClosedRule => result match {
            //when we use the topK approach then we enqueue the closed rule into the queue only if:
            // - queue size is lower than topK value
            // - queue is full, we replace the rule with lowest HC by this closed rule if this closed rule has higher HC
            //finally if the queue is full then we rewrite the _minHeadCoverage variable with HC from the peek rule of queue (with lowest HC)
            case Left(result) =>
              if (result.size < topK) {
                result.enqueue(closedRule)
              } else if (closedRule.measures[Measure.HeadCoverage].value > result.head.measures[Measure.HeadCoverage].value) {
                result.dequeue()
                result.enqueue(closedRule)
              }
              if (result.size >= topK) _minHeadCoverage = result.head.measures[Measure.HeadCoverage].value
            case Right(result) => result += closedRule
          }
          case _ =>
        }
        //if rule length is lower than max rule length we can expand this rule with one atom
        //if we use the topK approach the _minHeadCoverage may change during mining; therefore we need to check minHC threshold for the current rule
        if (rule.ruleLength < maxRuleLength && (result.left.forall(_.size < topK) || rule.measures[Measure.HeadCoverage].value > _minHeadCoverage)) {
          //expand the rule and add all expanded variants into the queue
          howLong("Rule expansion", true)(for (rule <- rule.expand) queue.add(rule))
        }
      }
      result match {
        case Left(result) => result.dequeueAll
        case Right(result) => result
      }
    }

  }

}

object Amie {

  def apply()(implicit debugger: Debugger = Debugger.EmptyDebugger): RulesMining = (new Amie)
    .addThreshold(Threshold.MinSupport(100))
    .addThreshold(Threshold.MinHeadCoverage(0.01))
    .addThreshold(Threshold.MaxRuleLength(3))

}