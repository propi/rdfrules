package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.algorithm.consumer.TopKRuleConsumer.MinHeadCoverageUpdatedEvent
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{ForEach, TopKQueue}

import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class TopKRuleConsumer private(k: Int, allowOverflowIfSameHeadCoverage: Boolean) extends RuleConsumer {

  @volatile private var _listener: PartialFunction[RuleConsumer.Event, Unit] = PartialFunction.empty

  protected def listener: PartialFunction[RuleConsumer.Event, Unit] = _listener

  def withListener(listener: PartialFunction[RuleConsumer.Event, Unit]): RuleConsumer = {
    _listener = listener
    this
  }

  private val _result = Promise[ForEach[FinalRule]]()

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[FinalRule]]
    val job = new Runnable {
      def run(): Unit = try {
        val queue = new TopKQueue[FinalRule](k, allowOverflowIfSameHeadCoverage)(Ordering.by[FinalRule, Double](_.headCoverage).reverse)
        var stopped = false
        while (!stopped) {
          messages.take() match {
            case Some(rule) =>
              if (queue.enqueue(rule)) {
                nextConsumer.foreach(_.send(rule))
                if (queue.isFull) queue.head.map(_.headCoverage).map(MinHeadCoverageUpdatedEvent).foreach(invokeEvent)
              }
            case None => stopped = true
          }
        }
        _result.success(ForEach.from(queue.dequeueAll.toIndexedSeq))
      } catch {
        case th: Throwable => _result.failure(th)
      }
    }
    val thread = new Thread(job)
    thread.start()
    messages
  }

  def send(rule: FinalRule): Unit = messages.put(Some(rule))

  def result: ForEach[FinalRule] = {
    messages.put(None)
    nextConsumer.foreach(_.result)
    Await.result(_result.future, 1 minute)
  }

}

object TopKRuleConsumer {

  case class MinHeadCoverageUpdatedEvent(minHeadCoverage: Double) extends RuleConsumer.Event

  private def normK(k: Int): Int = if (k < 1) 1 else k

  private def apply[T](ruleConsumer: TopKRuleConsumer)(f: TopKRuleConsumer => T): T = try {
    f(ruleConsumer)
  } finally {
    ruleConsumer.result
  }

  def apply[T](k: Int, allowOverflowIfSameHeadCoverage: Boolean)(f: TopKRuleConsumer => T): T = apply(new TopKRuleConsumer(normK(k), allowOverflowIfSameHeadCoverage))(f)

  def apply[T](k: Int)(f: TopKRuleConsumer => T): T = apply(k, false)(f)

}