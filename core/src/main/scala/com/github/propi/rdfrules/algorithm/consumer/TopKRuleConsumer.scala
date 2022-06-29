package com.github.propi.rdfrules.algorithm.consumer

import java.io.File
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.algorithm.consumer.TopKRuleConsumer.MinHeadCoverageUpdatedEvent
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.utils.{ForEach, TopKQueue}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class TopKRuleConsumer private(k: Int, allowOverflowIfSameHeadCoverage: Boolean, prettyPrintedFile: Option[(File, File => PrettyPrintedWriter)]) extends RuleConsumer {

  @volatile private var _listener: PartialFunction[RuleConsumer.Event, Unit] = PartialFunction.empty

  protected def listener: PartialFunction[RuleConsumer.Event, Unit] = _listener

  def withListener(listener: PartialFunction[RuleConsumer.Event, Unit]): RuleConsumer = {
    _listener = listener
    this
  }

  private val _result = Promise[RuleConsumer.Result]()

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[Rule]]
    val job = new Runnable {
      def run(): Unit = try {
        val queue = new TopKQueue[Rule.Simple](k, allowOverflowIfSameHeadCoverage)(Ordering.by[Rule.Simple, Double](_.headCoverage).reverse)
        val prettyPrintedWriter = prettyPrintedFile.map(x => x._2(x._1))
        try {
          val syncDuration = 10 seconds
          var stopped = false
          var lastSync = System.currentTimeMillis()
          var isAdded = false
          while (!stopped) {
            messages.poll(syncDuration.toSeconds, TimeUnit.SECONDS) match {
              case null =>
              case Some(rule) =>
                val ruleSimple = Rule.Simple(rule)
                if (queue.enqueue(ruleSimple)) {
                  if (queue.isFull) queue.head.map(_.headCoverage).map(MinHeadCoverageUpdatedEvent).foreach(invokeEvent)
                  prettyPrintedWriter.foreach(_.write(ruleSimple))
                  isAdded = true
                }
              case None => stopped = true
            }
            for (writer <- prettyPrintedWriter if isAdded && System.currentTimeMillis > (lastSync + syncDuration.toMillis)) {
              writer.flush()
              lastSync = System.currentTimeMillis()
              isAdded = false
            }
          }
        } finally {
          prettyPrintedWriter.foreach(_.close())
        }
        _result.success(RuleConsumer.Result(ForEach.from(queue.dequeueAll.toIndexedSeq)))
      } catch {
        case th: Throwable => _result.failure(th)
      }
    }
    val thread = new Thread(job)
    thread.start()
    messages
  }

  def send(rule: Rule): Unit = messages.put(Some(rule))

  def result: RuleConsumer.Result = {
    messages.put(None)
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

  def apply[T](k: Int)(f: TopKRuleConsumer => T): T = apply(new TopKRuleConsumer(normK(k), false, None))(f)

  def apply[T](k: Int, allowOverflowIfSameHeadCoverage: Boolean)(f: TopKRuleConsumer => T): T = apply(new TopKRuleConsumer(normK(k), allowOverflowIfSameHeadCoverage, None))(f)

  def apply[T](k: Int, allowOverflowIfSameHeadCoverage: Boolean, prettyPrintedFile: File, prettyPrintedWriterBuilder: File => PrettyPrintedWriter)(f: TopKRuleConsumer => T): T = {
    apply(new TopKRuleConsumer(normK(k), allowOverflowIfSameHeadCoverage, Some(prettyPrintedFile -> prettyPrintedWriterBuilder)))(f)
  }

}