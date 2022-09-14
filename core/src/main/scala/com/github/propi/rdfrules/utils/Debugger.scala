package com.github.propi.rdfrules.utils

import java.util.concurrent.atomic.AtomicInteger

import com.github.propi.rdfrules.utils.BasicFunctions.round
import com.github.propi.rdfrules.utils.Debugger.ActionDebugger
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait Debugger {

  val logger: Logger

  @volatile private var interrupted: Boolean = false

  def debug[T](name: String, num: Int = 0, forced: Boolean = false)(f: ActionDebugger => T): T

  def interrupt(): Unit = interrupted = true

  def isInterrupted: Boolean = interrupted

}

object Debugger {

  def apply[T](logger: Logger = Logger[Debugger])(f: Debugger => T): T = {
    f(new ActorDebugger(logger))
  }

  implicit object EmptyDebugger extends Debugger {
    val logger: Logger = Logger[Debugger]

    def debug[T](name: String, num: Int = 0, forced: Boolean = false)(f: ActionDebugger => T): T = f(EmptyActionDebugger)
  }

  class LoggerDebugger(val logger: Logger) extends Debugger {
    def debug[T](name: String, num: Int = 0, forced: Boolean = false)(f: ActionDebugger => T): T = f(EmptyActionDebugger)
  }

  private class ActorDebugger(val logger: Logger) extends Debugger {
    @volatile private var isActive = false

    def debug[T](name: String, num: Int = 0, forced: Boolean = false)(f: ActionDebugger => T): T = {
      if (isActive && !forced) {
        f(EmptyActionDebugger)
      } else {
        val ad = new ActorActionDebugger(name, num)
        val lastIsActive = isActive
        try {
          isActive = true
          val actor = new DebuggerActor(logger, ad)
          new Thread(actor).start()
          logger.info(ad.takeSnapshot.toString + " -- started")
          try {
            f(ad)
          } finally {
            actor.stop()
          }
        } finally {
          logger.info(ad.takeSnapshot.toString + " -- ended")
          isActive = lastIsActive
        }
      }
    }
  }

  sealed trait ActionDebugger {
    def done(msg: String = ""): Unit

    def result[T](msg: String = "")(f: => T): T = {
      val x = f
      done(msg)
      x
    }
  }

  private class ActorActionDebugger(name: String, num: Int) extends ActionDebugger {
    private val currentNum = new AtomicInteger(0)
    @volatile private var _currentMessage = ""

    case class Snapshot(absoluteProgress: Int, currentMessage: String) {
      def hasProgressBar: Boolean = num > 0

      def relativeProgress: Double = if (hasProgressBar) absoluteProgress.toDouble / num else 0.0

      override def toString: String = if (hasProgressBar) {
        s"Action $name, steps: $absoluteProgress of $num, progress: ${(relativeProgress * 100).floor}%"
      } else {
        s"Action $name, steps: $absoluteProgress"
      }
    }

    def takeSnapshot: Snapshot = Snapshot(currentNum.get(), _currentMessage)

    def done(msg: String = ""): Unit = {
      currentNum.incrementAndGet()
      _currentMessage = msg
    }
  }

  private object EmptyActionDebugger extends ActionDebugger {
    def done(msg: String = ""): Unit = {}
  }

  private class DebuggerActor(logger: Logger, action: ActorActionDebugger) extends Runnable {
    private val debugClock: Long = (5 seconds).toMillis
    @volatile private var stopped = false
    private var lastDump = System.currentTimeMillis()
    private var lastNum = 0

    private def dump(): Unit = {
      val snapshot = action.takeSnapshot
      if (lastNum != snapshot.absoluteProgress) {
        val windowTime = System.currentTimeMillis() - lastDump
        val windowNum = snapshot.absoluteProgress - lastNum
        val rating = s"(${round((windowNum.toDouble / windowTime) * 1000, 2)} per sec)"
        logger.info(s"$snapshot $rating" + (if (snapshot.currentMessage.nonEmpty) " -- " + snapshot.currentMessage else ""))
      }
      lastDump = System.currentTimeMillis()
      lastNum = snapshot.absoluteProgress
    }

    def stop(): Unit = {
      stopped = true
      synchronized(notify())
    }

    def run(): Unit = {
      while (!stopped) {
        synchronized(wait(debugClock))
        if (!stopped) {
          dump()
        }
      }
    }
  }

}