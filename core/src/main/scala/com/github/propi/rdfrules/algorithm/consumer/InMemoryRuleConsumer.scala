package com.github.propi.rdfrules.algorithm.consumer

import java.io.File
import java.util.concurrent.{ConcurrentLinkedQueue, LinkedBlockingQueue, TimeUnit}

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class InMemoryRuleConsumer private(prettyPrintedFile: Option[(File, File => PrettyPrintedWriter)]) extends RuleConsumer {

  private val rules = new ConcurrentLinkedQueue[Rule.Simple]
  private val isSavingToDisk = prettyPrintedFile.isDefined

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[Rule.Simple]]
    val job = new Runnable {
      def run(): Unit = {
        val prettyPrintedWriter = prettyPrintedFile.map(x => x._2(x._1)).get
        try {
          val syncDuration = 10 seconds
          var stopped = false
          var lastSync = System.currentTimeMillis()
          var isAdded = false
          while (!stopped) {
            messages.poll(syncDuration.toSeconds, TimeUnit.SECONDS) match {
              case null =>
              case Some(rule) =>
                isAdded = true
                prettyPrintedWriter.write(rule)
              case None => stopped = true
            }
            if (isAdded && System.currentTimeMillis > (lastSync + syncDuration.toMillis)) {
              prettyPrintedWriter.flush()
              lastSync = System.currentTimeMillis()
              isAdded = false
            }
          }
        } finally {
          prettyPrintedWriter.close()
        }
      }
    }
    val thread = new Thread(job)
    thread.start()
    messages
  }

  def send(rule: Rule): Unit = {
    val ruleSimple = Rule.Simple(rule)
    rules.add(ruleSimple)
    if (isSavingToDisk) {
      messages.put(None)
    }
  }

  def result: RuleConsumer.Result = {
    if (isSavingToDisk) {
      messages.put(None)
    }
    RuleConsumer.Result(new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = rules.iterator().asScala.foreach(f)
    }, true)
  }

}

object InMemoryRuleConsumer {

  def apply[T](f: InMemoryRuleConsumer => T): T = f(new InMemoryRuleConsumer(None))

  def apply[T](prettyPrintedFile: File)(f: InMemoryRuleConsumer => T)(implicit prettyPrintedWriterBuilder: File => PrettyPrintedWriter): T = {
    val x = new InMemoryRuleConsumer(Some(prettyPrintedFile -> prettyPrintedWriterBuilder))
    try {
      f(x)
    } finally {
      x.result
    }
  }

}