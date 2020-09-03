package com.github.propi.rdfrules.algorithm.amie

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.{PimpedRule, Settings}
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.ExtendedRule.ClosedRule
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{Debugger, HashQueue, TypedKeyMap}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(_parallelism: Int = Runtime.getRuntime.availableProcessors(),
                   _thresholds: TypedKeyMap[Threshold] = TypedKeyMap(),
                   _constraints: TypedKeyMap[RuleConstraint] = TypedKeyMap(),
                   _patterns: List[RulePattern] = Nil)
                  (implicit debugger: Debugger, ec: ExecutionContext) extends RulesMining(_parallelism, _thresholds, _constraints, _patterns) {

  self =>

  protected def transform(thresholds: TypedKeyMap[Threshold],
                          constraints: TypedKeyMap[RuleConstraint],
                          patterns: List[RulePattern],
                          parallelism: Int): RulesMining = new Amie(parallelism, thresholds, constraints, patterns)

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): IndexedSeq[Rule.Simple] = {
    val logger = debugger.logger
    //create amie process with debugger and final triple index
    implicit val settings: RuleRefinement.Settings = new Settings(this)(/*if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) */ debugger /* else Debugger.EmptyDebugger*/ , mapper)
    val process = new AmieProcess()
    try {
      process.searchRules()
      val timeoutReached = settings.timeout.exists(settings.currentDuration >= _)
      val rules = Await.result(process.result.getResult, 1 minute)
      if (timeoutReached) {
        logger.warn(s"The timeout limit '${thresholds.apply[Threshold.Timeout].duration.toMinutes} minutes' has been exceeded during mining. The miner returns ${rules.size} rules which need not be complete.")
      }
      if (debugger.isInterrupted) {
        logger.warn(s"The mining task has been interrupted. The miner returns ${rules.size} rules which need not be complete.")
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
    def isRefinable(rule: ExtendedRule): Boolean = topK <= 0 || rule.measures[Measure.Support].value >= settings.minComputedSupport(rule)

    /**
      * We add rule synchronously and "thread-safely" into the result set.
      * The rule is safely and quickly added into the message buffer.
      * Another thread manages messages and constructs a final rules buffer or priority queue.
      *
      * when we use the topK approach then we enqueue the closed rule into the queue only if:
      *  - queue size is lower than topK value
      *  - queue is full, we replace the rule with lowest HC by this closed rule if this closed rule has higher HC
      *    finally if the queue is full then we rewrite the minHeadCoverage variable in Settings with HC from the peek rule of queue (with lowest HC)
      *
      * @param rule closed rule
      */
    def addRule(rule: ClosedRule): Unit = messages.put(Some(rule))

  }

  private class AmieProcess(implicit val tripleIndex: TripleIndex[Int], val settings: RuleRefinement.Settings, val forAtomMatcher: AtomPatternMatcher[Atom]) extends HeadsFetcher {

    val patterns: List[RulePattern] = self.patterns
    val thresholds: TypedKeyMap.Immutable[Threshold] = self.thresholds

    val result: AmieResult = new AmieResult(settings)

    /**
      * Search all expanded rules from a specific rule which satisfies all thresholds, constraints and patterns.
      *
      * @return expanded closed rules in the result set
      */
    def searchRules(): Unit = debugger.debug("Amie rules mining") { ad =>
      debugger.debug("Browsed projections large buckets") { implicit _ad =>
        //queue for mined rules which can be also expanded
        //in this queue there can not be any duplicit rules (it speed up computation)
        val queue = new HashQueue[ExtendedRule]

        def queueSize = queue.synchronized {
          queue.size
        }

        val lastStageResult = collection.mutable.HashSet.empty[ClosedRule]
        //add all possible head to the queue
        getHeads foreach queue.add
        //first we refine all rules with length 1 (stage 1)
        //once all rules with length 1 are refined we go to the stage 2 (refine rules with length 2), etc.
        var stage = 1
        val foundRules = new AtomicInteger(0)
        //if the queue is not empty and the timeout is not reached, go to the stage X
        while (!queue.isEmpty && settings.timeout.forall(_ > settings.currentDuration) && !debugger.isInterrupted) {
          //starts P jobs in parallel where P is number of processors
          //each job refines rules from queue with length X where X is the stage number.
          val jobs = for (_ <- 0 until parallelism) yield Future {
            Stream.continually {
              //poll head rule from queue with length X (wher X is the stage number)
              //if queue is empty or head rule has length greater than X, return None
              queue.synchronized {
                if (!queue.isEmpty && queue.peek.ruleLength == stage) Some(queue.poll) else None
              }
            }.takeWhile(_.isDefined && settings.timeout.forall(_ > settings.currentDuration) && !debugger.isInterrupted).map(_.get).filter(result.isRefinable).foreach { rule =>
              ad.done(s"processed rules, found closed rules: ${foundRules.get()}, queue size: $queueSize")
              //we continually take all defined (and valid) rules from queue until end of the stage or reaching of the timeout
              //if rule length is lower than max rule length we can expand this rule with one atom (in this refine phase it always applies)
              //if we use the topK approach the minHeadCoverage may change during mining; therefore we need to check minHC threshold for the current rule
              //refine the rule and add all expanded variants into the queue
              if (stage + 1 < settings.maxRuleLength) {
                /*howLong("Rule expansion", true)(*/ for (rule <- rule.refine) {
                  val (beforeSize, currentSize) = queue.synchronized {
                    val beforeSize = queue.size
                    queue.add(rule)
                    beforeSize -> queue.size
                  }
                  if (currentSize > beforeSize && rule.isInstanceOf[ClosedRule]) {
                    result.addRule(rule.asInstanceOf[ClosedRule])
                    foundRules.incrementAndGet()
                  }
                } //)
              } else {
                /*howLong("Rule expansion", true)(*/ for (rule@ClosedRule(_, _) <- rule.refine) {
                  val (beforeSize, currentSize) = lastStageResult.synchronized {
                    val beforeSize = lastStageResult.size
                    lastStageResult += rule
                    beforeSize -> lastStageResult.size
                  }
                  if (currentSize > beforeSize) {
                    result.addRule(rule)
                    ad.done(s"processed rules, found closed rules: ${foundRules.incrementAndGet()}, queue size: $queueSize")
                  }
                } //)
              }
            }
          }
          //wait for all jobs in the current stage
          Await.result(Future.sequence(jobs), Duration.Inf)
          //stage is completed, go to the next stage
          stage += 1
        }
      }
    }

  }

}

object Amie {

  /**
    * Create an AMIE+ miner. If you do not specify any threshold, default is minHeadSize = 100, minSupport = 1, maxRuleLength = 3
    *
    * @param debugger debugger
    * @param ec       ec
    * @return
    */
  def apply()(implicit debugger: Debugger, ec: ExecutionContext = ExecutionContext.global): RulesMining = new Amie()

}