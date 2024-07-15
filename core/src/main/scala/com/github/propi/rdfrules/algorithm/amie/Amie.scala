package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.RuleRefinement.PimpedRule
import com.github.propi.rdfrules.algorithm.consumer.TopKRuleConsumer
import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.index.{IntervalsIndex, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.ExpandingRule.{ClosedRule, HeadTriplesBootstrapper}
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.{Bootstrapper, Debugger, ForEach, TypedKeyMap, UniqueQueue}

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(_parallelism: Int = Runtime.getRuntime.availableProcessors(),
                   _thresholds: TypedKeyMap.Mutable[Threshold] = TypedKeyMap.Mutable(),
                   _constraints: TypedKeyMap.Mutable[RuleConstraint] = TypedKeyMap.Mutable(),
                   _patterns: List[RulePattern] = Nil,
                   _experiment: Boolean = false)
                  (implicit debugger: Debugger) extends RulesMining(_parallelism, _thresholds, _constraints, _patterns, _experiment) {

  self =>

  @volatile private var topKActivated: Boolean = false

  protected def transform(thresholds: TypedKeyMap.Mutable[Threshold],
                          constraints: TypedKeyMap.Mutable[RuleConstraint],
                          patterns: List[RulePattern],
                          parallelism: Int,
                          experiment: Boolean): RulesMining = new Amie(parallelism, thresholds, constraints, patterns, experiment)

  /**
    * Mine all closed rules from tripleIndex by defined thresholds (hc, support, rule length), optional rule pattern and constraints
    *
    * @param tripleIndex dataset from which rules are mined
    * @return Mined closed rules which satisfies all thresholds, patterns and constraints.
    *         Rules contains only these measures: head size, head coverage and support
    *         Rules are not ordered
    */
  def mine(ruleConsumer: RuleConsumer)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex, intervals: IntervalsIndex): ForEach[FinalRule] = {
    val logger = debugger.logger
    val headTriplesGenerator: (Option[HeadTriplesBootstrapper] => ForEach[FinalRule]) => ForEach[FinalRule] = if (thresholds.get[Threshold.LocalTimeout].exists(x => x.hasDuration || x.hasMarginError)) {
      f => Bootstrapper[(Int, Option[TripleItemPosition[Int]]), (Int, Int), ForEach[FinalRule]]()(x => f(Some(x)))
    } else {
      f => f(None)
    }
    headTriplesGenerator { bootstrapper =>
      implicit val settings: AmieSettings = new AmieSettings(this, bootstrapper)(/*if (logger.underlying.isDebugEnabled && !logger.underlying.isTraceEnabled) */ debugger /* else Debugger.EmptyDebugger*/ , mapper, intervals)
      val observedRuleConsumer = ruleConsumer.withListener {
        case TopKRuleConsumer.MinHeadCoverageUpdatedEvent(minHeadCoverage) =>
          topKActivated = true
          settings.setMinHeadCoverage(minHeadCoverage)
      }
      //create amie process with debugger and final triple index
      val process = new AmieProcess(observedRuleConsumer)
      logger.info("Amie task settings:\n" + settings)
      process.searchRules()
      val timeoutReached = settings.timeout.exists(settings.currentDuration >= _)
      val result = observedRuleConsumer.result
      if (timeoutReached) {
        logger.warn(s"The timeout limit '${thresholds.apply[Threshold.Timeout].duration.toMinutes} minutes' has been exceeded during mining. The miner returns ${process.getFoundRules} rules which need not be complete.")
      }
      if (debugger.isInterrupted) {
        logger.warn(s"The mining task has been interrupted. The miner returns ${process.getFoundRules} rules which need not be complete.")
      }
      result
    }
  }

  /*private class AmieResult(settings: RuleRefinement.Settings) extends Runnable {

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
          } else if (rule.headCoverage > queue.head.measures[Measure.HeadCoverage].value) {
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
    def isRefinable(rule: ExtendedRule): Boolean = topK <= 0 || rule.support >= settings.minComputedSupport(rule)

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

  }*/

  private class AmieProcess(ruleConsumer: RuleConsumer)(implicit val tripleIndex: TripleIndex[Int], val tripleItemIndex: TripleItemIndex, val settings: AmieSettings, val forAtomMatcher: MappedAtomPatternMatcher[Atom]) extends HeadsFetcher {
    private val foundRules = new AtomicInteger(0)

    def getFoundRules: Int = foundRules.get()

    /**
      * It checks whether a rule may be refinable by topK approach.
      * If the topK is turned off then the rule is always refinable.
      * If the topK is turned on and the rule has reached the minimal head coverage (by the lowest entity in the queue), then it is refinable.
      *
      * @param rule a rule to be refinable
      * @return true = is refinable, false = do not refine it!
      */
    private def isRefinable(rule: ExpandingRule): Boolean = !topKActivated || rule.support >= settings.minComputedSupport(rule)

    @tailrec
    private def executeStage(stage: Int, queue: UniqueQueue[ExpandingRule]): Unit = {
      val nextQueue = /*if (experiment) new UniqueQueue.ConcurrentLinkedQueueWrapper(new ConcurrentLinkedQueue[ExpandingRule]()) else */ new UniqueQueue.ThreadSafeUniqueSet[ExpandingRule]
      val duplicates = new AtomicInteger(0)
      debugger.debug(s"Amie rules mining, stage $stage of ${settings.maxRuleLength - 1}", queue.size) { ad =>
        val activeThreads = new AtomicInteger(parallelism)
        //starts P jobs in parallel where P is number of processors
        //each job refines rules from queue with length X where X is the stage number.
        val jobs = List.fill(parallelism) {
          val job: Runnable = () => try {
            Iterator.continually(queue.pollOption).takeWhile(_.isDefined && settings.timeout.forall(_ > settings.currentDuration) && !debugger.isInterrupted).map(_.get).filter(isRefinable).foreach { rule =>
              //debug()
              //we continually take all defined (and valid) rules from queue until end of the stage or reaching of the timeout
              //if rule length is lower than max rule length we can expand this rule with one atom (in this refine phase it always applies)
              //if we use the topK approach the minHeadCoverage may change during mining; therefore we need to check minHC threshold for the current rule
              //refine the rule and add all expanded variants into the queue
              for (rule <- rule.refine) {
                val ruleIsAdded = nextQueue.add(rule)
                if (!ruleIsAdded) {
                  duplicates.incrementAndGet()
                  //println(Stringifier(rule.asInstanceOf[Rule]))
                }
                if (ruleIsAdded && rule.isInstanceOf[ClosedRule] && (settings.patterns.isEmpty || settings.patterns.exists(_.matchWith[Rule](rule)))) {
                  ruleConsumer.send(rule.toFinalRule)
                  foundRules.incrementAndGet()
                }
              }
              ad.done(s"processed rules, found closed rules: ${foundRules.get()}, activeThreads: ${activeThreads.get()}, duplicates: ${duplicates.get()}")
            }
          } finally {
            activeThreads.decrementAndGet()
          }
          val thread = new Thread(job)
          thread.start()
          thread
        }
        //wait for all jobs in the current stage
        for (job <- jobs) {
          job.join()
        }
      }
      logger.info(s"total duplicates in stage $stage: ${duplicates.get()}")
      //println(s"total duplicates: ${duplicates.get()}")
      //stage is completed, go to the next stage
      if (stage + 1 < settings.maxRuleLength && !nextQueue.isEmpty) {
        executeStage(stage + 1, nextQueue)
      }
    }

    /**
      * Search all expanded rules from a specific rule which satisfies all thresholds, constraints and patterns.
      *
      * @return expanded closed rules in the result set
      */
    def searchRules(): Unit = {
      //queue for mined rules which can be also expanded
      //in this queue there can not be any duplicit rules (it speed up computation)
      //add all possible head to the queue
      val queue = new ConcurrentLinkedQueue[ExpandingRule]()
      getHeads.foreach(queue.add)
      //first we refine all rules with length 1 (stage 1)
      //once all rules with length 1 are refined we go to the stage 2 (refine rules with length 2), etc.
      if (settings.maxRuleLength > 1 && !queue.isEmpty) {
        executeStage(1, queue)
      }
    }

  }

}

object Amie {

  /**
    * Create an AMIE+ miner. If you do not specify any threshold, default is minHeadSize = 100, minSupport = 1, maxRuleLength = 3
    *
    * @param debugger debugger
    * @return
    */
  def apply()(implicit debugger: Debugger): RulesMining = new Amie()

}