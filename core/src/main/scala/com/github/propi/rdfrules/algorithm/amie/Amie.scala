package com.github.propi.rdfrules.algorithm.amie

import java.util.concurrent.LinkedBlockingQueue

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.{Settings, _}
import com.github.propi.rdfrules.index.{TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.ExtendedRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.HowLong._
import com.github.propi.rdfrules.utils.{Debugger, HashQueue, TypedKeyMap}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(parallelism: Int,
                   _thresholds: TypedKeyMap[Threshold] = TypedKeyMap(),
                   _constraints: TypedKeyMap[RuleConstraint] = TypedKeyMap(),
                   _patterns: List[RulePattern] = Nil)
                  (implicit debugger: Debugger, ec: ExecutionContext) extends RulesMining(_thresholds, _constraints, _patterns) {

  protected def transform(thresholds: TypedKeyMap[Threshold],
                          constraints: TypedKeyMap[RuleConstraint],
                          patterns: List[RulePattern]): RulesMining = new Amie(parallelism, thresholds, constraints, patterns)

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(implicit tripleIndex: TripleHashIndex, mapper: TripleItemHashIndex): IndexedSeq[Rule.Simple] = {
    val logger = debugger.logger
    //create amie process with debugger and final triple index
    implicit val settings: RuleRefinement.Settings = new Settings(this)(if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) debugger else Debugger.EmptyDebugger, mapper)
    val process = new AmieProcess()
    try {
      //get all possible heads
      /*val heads = {
        val builder = ParVector.newBuilder[DanglingRule]
        builder ++= process.getHeads
        builder.result()
      }*/
      //val heads = process.getHeads.toIndexedSeq
      //parallel rule mining: for each head we search rules
      /*debugger.debug("Amie rules mining", heads.length) { ad =>
        //Await.result(Workers(heads)(head => ad.result()(process.searchRules(head))), Duration.Inf)
        //heads.foreach(head => ad.result()(process.searchRules(head)))
        heads.parForeach()(head => ad.result()(process.searchRules(head)))
      }*/
      process.searchRules()
      val timeoutReached = process.timeout.exists(process.currentDuration >= _)
      val rules = Await.result(process.result.getResult, 1 minute)
      if (timeoutReached) {
        logger.warn(s"The timeout limit '${thresholds.apply[Threshold.Timeout].duration.toMinutes} minutes' has been exceeded during mining. The miner returns ${rules.size} rules which need not be complete.")
      }
      rules
    } finally {
      process.result.close()
    }
  }

  private class AmieResult(settings: RuleRefinement.Settings) extends Runnable {

    private val messages = new LinkedBlockingQueue[Option[ClosedRule]]
    private val topK: Int = thresholds.get[Threshold.TopK].map(_.value).getOrElse(0)
    private val result = Promise[IndexedSeq[Rule.Simple]]
    private val thread = {
      val x = new Thread(this)
      x.start()
      x
    }

    def run(): Unit = try {
      var stopped = false
      /**
        * collection for mined closed rules
        * there are two collections for two mining approaches: byMinSupport or topK
        * for topK we need to use priority queue where on the peek there is rule with lowest HC
        * for byMinSupport we need only a simple buffer
        */
      val result = if (topK > 0) {
        Left(mutable.PriorityQueue.empty[Rule.Simple](Ordering.by[Rule.Simple, Double](_.measures[Measure.HeadCoverage].value).reverse))
      } else {
        Right(mutable.ArrayBuffer.empty[Rule.Simple])
      }
      val addRule: ClosedRule => Unit = result match {
        case Left(queue) => rule => {
          var enqueued = false
          if (queue.size < topK) {
            queue.enqueue(rule)
            enqueued = true
          } else if (rule.measures[Measure.HeadCoverage].value > queue.head.measures[Measure.HeadCoverage].value) {
            queue.dequeue()
            queue.enqueue(rule)
            enqueued = true
          }
          if (queue.size >= topK && enqueued) settings.setMinHeadCoverage(queue.head.measures[Measure.HeadCoverage].value)
        }
        case Right(buffer) => rule => buffer += rule
      }
      while (!stopped) {
        messages.take() match {
          case Some(rule) => addRule(rule)
          case None => stopped = true
        }
      }
      this.result.success(result.fold(_.dequeueAll, x => x))
    } catch {
      case th: Throwable => this.result.failure(th)
    }

    /**
      * Kill the consumer thread.
      */
    def close(): Unit = thread.interrupt()

    /**
      * Stop consuming of messages and return future with result.
      * It sends a stop message into the message queue.
      * After the consumer process this message the consumer thread will stop and a result will be passed into the future object.
      *
      * @return indexed seq of rules
      */
    def getResult: Future[IndexedSeq[Rule.Simple]] = {
      messages.put(None)
      result.future
    }

    /**
      * It checks whether a rule may be refinable by topK approach.
      * If the topK is turned off then the rule is always refinable.
      * If the topK is turned on and the rule has reached the minimal head coverage (by the lowest entity in the queue), then it is refinable.
      *
      * @param rule a rule to be refinable
      * @return true = is refinable, false = do not refine it!
      */
    def isRefinable(rule: Rule): Boolean = topK <= 0 || rule.measures[Measure.HeadCoverage].value >= settings.minHeadCoverage

    /**
      * We add rule synchronously and "thread-safely" into the result set.
      * The rule is safely and quickly added into the message buffer.
      * Another thread manages messages and constructs a final rules buffer or priority queue.
      *
      * when we use the topK approach then we enqueue the closed rule into the queue only if:
      *  - queue size is lower than topK value
      *  - queue is full, we replace the rule with lowest HC by this closed rule if this closed rule has higher HC
      * finally if the queue is full then we rewrite the minHeadCoverage variable in Settings with HC from the peek rule of queue (with lowest HC)
      *
      * @param rule closed rule
      */
    def addRule(rule: ClosedRule): Unit = messages.put(Some(rule))

  }

  private class AmieProcess(implicit val tripleIndex: TripleHashIndex, settings: RuleRefinement.Settings, forAtomMatcher: AtomPatternMatcher[Atom]) extends AtomCounting {

    private val minSupport: Int = thresholds.apply[Threshold.MinHeadSize].value
    private val startTime = System.currentTimeMillis()
    val timeout: Option[Long] = thresholds.get[Threshold.Timeout].map(_.duration.toMillis)
    val result: AmieResult = new AmieResult(settings)

    def currentDuration: Long = System.currentTimeMillis() - startTime

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
              specifySubject(atom).map(_.transform(`object` = Atom.Variable(0)))
            }
            val it2 = specifyObject(atom)
            Iterator(atom) ++ it1 ++ it2
          }
        } else {
          it
        }
      }
      //filter all atoms by patterns and join all valid patterns to the atom
      possibleAtoms.flatMap { atom =>
        val validPatterns = settings.patterns.filter(rp => rp.consequent.forall(atomPattern => forAtomMatcher.matchPattern(atom, atomPattern)))
        if (patterns.isEmpty || validPatterns.nonEmpty) {
          Iterator(atom -> validPatterns)
        } else {
          Iterator.empty
        }
      }.flatMap { case (atom, patterns) =>
        //convert all atoms to rules and filter it by min support
        val tm = tripleIndex.predicates(atom.predicate)
        val danglingRule = Some(atom.subject, atom.`object`).collect {
          case (v1: Atom.Variable, v2: Atom.Variable) => (ExtendedRule.TwoDanglings(v1, v2, Nil), tm.size, tm.size)
          case (Atom.Constant(c), v1: Atom.Variable) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.subjects.get(c).map(_.size).getOrElse(0))
          case (v1: Atom.Variable, Atom.Constant(c)) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.objects.get(c).map(_.size).getOrElse(0))
        }.filter(x => x._3 >= math.max(minSupport, settings.minHeadCoverage * x._2)).map(x =>
          DanglingRule(Vector.empty, atom)(
            TypedKeyMap(Measure.HeadSize(x._2), Measure.Support(x._3), Measure.HeadCoverage(x._3.toDouble / x._2)),
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
      * @return expanded closed rules in the result set
      */
    def searchRules(): Unit = debugger.debug("Amie rules mining") { ad =>
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      val queue = new HashQueue[ExtendedRule]
      //add all possible head to the queue
      getHeads foreach queue.add
      //first we refine all rules with length 1 (stage 1)
      //once all rules with length 1 are refined we go to the stage 2 (refine rules with length 2), etc.
      var stage = 1
      //if the queue is not empty and the timeout is not reached, go to the stage X
      while (!queue.isEmpty && timeout.forall(_ > currentDuration)) {
        if (stage >= settings.maxRuleLength) {
          //if it is the final stage then it means no refinements, we only add all closed rules to the result set
          while (!queue.isEmpty) {
            queue.poll match {
              case closedRule: ClosedRule => result.addRule(closedRule)
              case _ =>
            }
          }
        } else {
          //refinement stage
          //starts P jobs in parallel where P is number of processors
          //each job refines rules from queue with length X where X is the stage number.
          val jobs = for (_ <- 0 until parallelism) yield Future {
            Stream.continually {
              //poll head rule from queue with length X (wher X is the stage number)
              //if queue is empty or head rule has length greater than X, return None
              queue.synchronized {
                if (!queue.isEmpty && queue.peek.ruleLength == stage) Some(queue.poll) else None
              }
            }.takeWhile(_.isDefined && timeout.forall(_ > currentDuration)).map(_.get).foreach { rule =>
              //we continually take all defined (and valid) rules from queue until end of the stage or reaching of the timeout
              //if the rule is closed we add it to the result set
              rule match {
                case closedRule: ClosedRule => result.addRule(closedRule)
                case _ =>
              }
              //if rule length is lower than max rule length we can expand this rule with one atom (in this refine phase it always applies)
              //if we use the topK approach the minHeadCoverage may change during mining; therefore we need to check minHC threshold for the current rule
              if (result.isRefinable(rule)) {
                //refine the rule and add all expanded variants into the queue
                howLong("Rule expansion", true)(for (rule <- rule.refine) {
                  val (beforeSize, currentSize) = queue.synchronized {
                    val beforeSize = queue.size
                    queue.add(rule)
                    beforeSize -> queue.size
                  }
                  if (currentSize > beforeSize && rule.isInstanceOf[ClosedRule]) ad.done(s"found rules (queue size: $currentSize)")
                  //if (currentSize > beforeSize) ad.done(s"refinements (queue size: $currentSize)")
                })
              }
            }
          }
          //wait for all jobs in the current stage
          Await.result(Future.sequence(jobs), Duration.Inf)
        }
        //stage is completed, go to the next stage
        stage += 1
      }
    }

  }

}

object Amie {

  def apply(parallelism: Int = Runtime.getRuntime.availableProcessors())(implicit debugger: Debugger, ec: ExecutionContext = ExecutionContext.global): RulesMining = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    new Amie(parallelism = normParallelism)
      .addThreshold(Threshold.MinHeadSize(100))
      .addThreshold(Threshold.MinHeadCoverage(0.01))
      .addThreshold(Threshold.MaxRuleLength(3))
  }

}