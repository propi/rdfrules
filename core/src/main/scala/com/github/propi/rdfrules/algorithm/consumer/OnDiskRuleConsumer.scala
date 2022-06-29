package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

import java.io.{File, FileInputStream, FileOutputStream, OutputStream}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class OnDiskRuleConsumer private(file: File, prettyPrintedFile: Option[(File, File => PrettyPrintedWriter)]) extends RuleConsumer.NoEventRuleConsumer {

  private val _result = Promise[RuleConsumer.Result]()

  private class BufferedOutputStream(stream: OutputStream) extends OutputStream {
    private val buffer = new Array[Byte](8192)
    private var pointer = 0

    def write(i: Int): Unit = throw new UnsupportedOperationException()

    def write(rule: Rule.Simple): Unit = {
      val bytes = Serializer.serialize(rule)
      if (bytes.length > buffer.length) {
        stream.write(bytes)
      } else if (pointer + bytes.length > buffer.length) {
        flush()
        Array.copy(bytes, 0, buffer, 0, bytes.length)
        pointer = bytes.length
      } else {
        Array.copy(bytes, 0, buffer, pointer, bytes.length)
        pointer += bytes.length
      }
    }

    override def flush(): Unit = {
      if (pointer > 0) {
        stream.write(buffer, 0, pointer)
        pointer = 0
      }
    }

    override def close(): Unit = {
      flush()
      stream.close()
    }
  }

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[Rule]]
    val job = new Runnable {
      def run(): Unit = try {
        val fos = new FileOutputStream(file)
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
                bos.write(ruleSimple)
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
        _result.success(RuleConsumer.Result((f: Rule.Simple => Unit) => {
          Deserializer.deserializeFromInputStream[Rule.Simple, Unit](new FileInputStream(file)) { reader =>
            Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
          }
        }))
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