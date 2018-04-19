package com.github.propi.rdfrules.algorithm.amie

import java.util.concurrent.LinkedBlockingQueue

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.{Settings, _}
import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.ExtendedRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.HowLong._
import com.github.propi.rdfrules.utils.{Debugger, HashQueue, TypedKeyMap}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.collection.parallel.immutable.ParVector
import scala.concurrent.ExecutionContext

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(logger: Logger)(implicit debugger: Debugger) extends RulesMining {

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(implicit tripleIndex: TripleHashIndex): IndexedSeq[Rule.Simple] = {
    //create amie process with debugger and final triple index
    val process = new AmieProcess()(tripleIndex, new Settings(this)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger))
    //get all possible heads
    val heads = {
      val builder = ParVector.newBuilder[DanglingRule]
      builder ++= process.getHeads
      builder.result()
    }
    //parallel rule mining: for each head we search rules
    debugger.debug("Amie rules mining", heads.length) { ad =>
      heads.foreach(head => ad.result()(process.searchRules(head)))
    }
    val rules = process.getResult
    if (process.timeout.exists(process.currentDuration >= _)) {
      logger.warn(s"The timeout limit '${thresholds.apply[Threshold.Timeout].duration.toMinutes} minutes' has been exceeded during mining. The miner returns ${rules.size} rules which need not be complete.")
    }
    rules
  }

  private class AmieResult extends Runnable {
    private val messages = new LinkedBlockingQueue[Option[ClosedRule]]

    def run(): Unit = {
      var stopped = false
      while (!stopped) {
        messages.take() match {
          case Some(rule) => 
          case None => stopped = true
        }
      }
    }

    def addRule(rule: ClosedRule): Unit = messages.put(Some(rule))
  }

  private class AmieProcess(implicit val tripleIndex: TripleHashIndex, settings: RuleRefinement.Settings) extends AtomCounting {

    private implicit val ec: ExecutionContext = ExecutionContext.global

    private val topK: Int = thresholds.get[Threshold.TopK].map(_.value).getOrElse(0)
    private val minSupport: Int = thresholds.apply[Threshold.MinHeadSize].value
    private val startTime = System.currentTimeMillis()
    val timeout: Option[Long] = thresholds.get[Threshold.Timeout].map(_.duration.toMillis)

    def currentDuration: Long = System.currentTimeMillis() - startTime

    /**
      * collection for mined closed rules
      * there are two collections for two mining approaches: byMinSupport or topK
      * for topK we need to use priority queue where on the peek there is rule with lowest HC
      * for byMinSupport we need only a simple buffer
      */
    private val result: Either[mutable.PriorityQueue[Rule.Simple], mutable.ArrayBuffer[Rule.Simple]] = if (topK > 0) {
      Left(mutable.PriorityQueue.empty[Rule.Simple](Ordering.by[Rule.Simple, Double](_.measures[Measure.HeadCoverage].value).reverse))
    } else {
      Right(mutable.ArrayBuffer.empty[Rule.Simple])
    }


    /**
      * Thread safe method for getting result
      *
      * @return indexed seq of rules
      */
    def getResult: IndexedSeq[Rule.Simple] = result.synchronized(result.fold(_.dequeueAll, x => x))

    /**
      * We add rule synchronously and "thread-safely" into the result set
      *
      * when we use the topK approach then we enqueue the closed rule into the queue only if:
      *  - queue size is lower than topK value
      *  - queue is full, we replace the rule with lowest HC by this closed rule if this closed rule has higher HC
      * finally if the queue is full then we rewrite the minHeadCoverage variable in Settings with HC from the peek rule of queue (with lowest HC)
      *
      * @param rule closed rule
      */
    private def addRule(rule: ClosedRule): Unit = result.synchronized {
      //TODO how to do it asynchronously
      //- it is not possible to use Future in this function because locks are queued. Mining is end but rules are still adding...
      result match {
        case Left(result) =>
          var enqueued = false
          if (result.size < topK) {
            result.enqueue(rule)
            enqueued = true
          } else if (rule.measures[Measure.HeadCoverage].value > result.head.measures[Measure.HeadCoverage].value) {
            result.dequeue()
            result.enqueue(rule)
            enqueued = true
          }
          if (result.size >= topK && enqueued) settings.setMinHeadCoverage(result.head.measures[Measure.HeadCoverage].value)
        case Right(result) => result += rule
      }
    }

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
        val it = tripleIndex.predicates.keysIterator.filter(settings.isValidPredicate).map(Atom(Atom.Variable(0), _, Atom.Variable(1)))
        if (settings.isWithInstances) {
          //if instantiated atoms are allowed then we specify variables by constants
          //only one variable may be replaced by a constant
          //result is original variable atom + instantied atoms with constants in subject + instantied atoms with constants in object
          //we do not instantient subject variable if onlyObjectInstances is true
          it.flatMap { atom =>
            val it1 = if (settings.onlyObjectInstances) {
              Iterator.empty
            } else {
              specifySubject(atom).map(_.copy(`object` = Atom.Variable(0)))
            }
            val it2 = specifyObject(atom)
            Iterator(atom) ++ it1 ++ it2
          }
        } else {
          it
        }
      }
      val forAtomMatcher = new AtomPatternMatcher.ForAtom
      //filter all atoms by patterns and join all valid patterns to the atom
      possibleAtoms.flatMap { atom =>
        val validPatterns = patterns.filter(rp => rp.consequent.forall(atomPattern => forAtomMatcher.matchPattern(atom, atomPattern)))
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
    def searchRules(initRule: ExtendedRule): Unit = {
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      val queue = new HashQueue[ExtendedRule].add(initRule)
      //if the queue is not empty, try expand rules
      while (!queue.isEmpty && timeout.forall(_ > currentDuration)) {
        //get one rule from queue and remove it from that
        val rule = queue.poll
        //if the rule is closed we add it to the result set
        rule match {
          case closedRule: ClosedRule => addRule(closedRule)
          case _ =>
        }
        //if rule length is lower than max rule length we can expand this rule with one atom
        //if we use the topK approach the minHeadCoverage may change during mining; therefore we need to check minHC threshold for the current rule
        if (rule.ruleLength < settings.maxRuleLength && (result.left.forall(_.size < topK) || rule.measures[Measure.HeadCoverage].value > settings.minHeadCoverage)) {
          //refine the rule and add all expanded variants into the queue
          howLong("Rule expansion", true)(for (rule <- rule.refine) queue.add(rule))
        }
      }
    }

  }

}

object Amie {

  def apply(logger: Logger = Logger[Amie])(implicit debugger: Debugger = Debugger.EmptyDebugger): RulesMining = new Amie(logger)
    .addThreshold(Threshold.MinHeadSize(100))
    .addThreshold(Threshold.MinHeadCoverage(0.01))
    .addThreshold(Threshold.MaxRuleLength(3))

}