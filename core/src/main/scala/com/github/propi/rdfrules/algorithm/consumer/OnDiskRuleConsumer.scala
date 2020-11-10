package com.github.propi.rdfrules.algorithm.consumer

import java.io.{BufferedOutputStream, File, FileInputStream, FileOutputStream}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class OnDiskRuleConsumer private(file: File, prettyPrintedFile: Option[(File, File => PrettyPrintedWriter)]) extends RuleConsumer.NoEventRuleConsumer {

  private val _result = Promise[RuleConsumer.Result]

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[Rule]]
    val job = new Runnable {
      def run(): Unit = try {
        val fos = new FileOutputStream(file)
        //TODO problem with buffers. It can flush not readable buckets so after crash it can be corrupted
        //create own buffer and after some time flush and sync
        val bos = new BufferedOutputStream(fos)
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
                bos.write(Serializer.serialize(ruleSimple))
                isAdded = true
                prettyPrintedWriter.foreach(_.write(ruleSimple))
              case None => stopped = true
            }
            if (isAdded && System.currentTimeMillis > (lastSync + syncDuration.toMillis)) {
              bos.flush()
              fos.getFD.sync()
              prettyPrintedWriter.foreach(_.flush())
              lastSync = System.currentTimeMillis()
              isAdded = false
            }
          }
        } finally {
          bos.close()
          prettyPrintedWriter.foreach(_.close())
        }
        _result.success(RuleConsumer.Result(new Traversable[Rule.Simple] {
          def foreach[U](f: Rule.Simple => U): Unit = {
            Deserializer.deserializeFromInputStream[Rule.Simple, Unit](new FileInputStream(file)) { reader =>
              Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
            }
          }
        }, false))
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

object OnDiskRuleConsumer {

  private def apply[T](ruleConsumer: OnDiskRuleConsumer)(f: OnDiskRuleConsumer => T): T = try {
    f(ruleConsumer)
  } finally {
    ruleConsumer.result
  }

  def apply[T](file: File)(f: OnDiskRuleConsumer => T): T = apply(new OnDiskRuleConsumer(file, None))(f)

  def apply[T](file: File, prettyPrintedFile: File, prettyPrintedWriterBuilder: File => PrettyPrintedWriter)(f: OnDiskRuleConsumer => T): T = {
    apply(new OnDiskRuleConsumer(file, Some(prettyPrintedFile -> prettyPrintedWriterBuilder)))(f)
  }

}