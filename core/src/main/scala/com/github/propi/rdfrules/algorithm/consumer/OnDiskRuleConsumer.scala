package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.ForEach

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class OnDiskRuleConsumer private(ruleIO: RuleIO) extends RuleConsumer.NoEventRuleConsumer {

  private val _result = Promise[ForEach[FinalRule]]()

  /*private class BufferedOutputStream(stream: OutputStream) extends OutputStream {
    private val buffer = new Array[Byte](8192)
    private var pointer = 0

    def write(i: Int): Unit = throw new UnsupportedOperationException()

    def write(rule: FinalRule): Unit = {
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
  }*/

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[Rule]]
    val job = new Runnable {
      def run(): Unit = try {
        ruleIO.writer { writer =>
          val syncDuration = 10 seconds
          var stopped = false
          var lastSync = System.currentTimeMillis()
          var isAdded = false
          while (!stopped) {
            messages.poll(syncDuration.toSeconds, TimeUnit.SECONDS) match {
              case null =>
              case Some(rule) =>
                writer.write(rule)
                nextConsumer.foreach(_.send(rule))
                isAdded = true
              case None => stopped = true
            }
            if (isAdded && System.currentTimeMillis > (lastSync + syncDuration.toMillis)) {
              writer.flush()
              lastSync = System.currentTimeMillis()
              isAdded = false
            }
          }
        }
        _result.success((f: FinalRule => Unit) => {
          ruleIO.reader { reader =>
            Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
          }
        })
      } catch {
        case th: Throwable => _result.failure(th)
      }
    }
    val thread = new Thread(job)
    thread.start()
    messages
  }

  def send(rule: Rule): Unit = messages.put(Some(rule))

  def result: ForEach[FinalRule] = {
    messages.put(None)
    nextConsumer.foreach(_.result)
    Await.result(_result.future, 1 minute)
  }

}

object OnDiskRuleConsumer {

  private def apply[T](ruleConsumer: OnDiskRuleConsumer)(f: OnDiskRuleConsumer => T): T = try {
    f(ruleConsumer)
  } finally {
    ruleConsumer.result
  }

  def apply[T](ruleIO: RuleIO)(f: OnDiskRuleConsumer => T): T = apply(new OnDiskRuleConsumer(ruleIO))(f)

}