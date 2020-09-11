package com.github.propi.rdfrules.utils

import java.util.concurrent.LinkedBlockingQueue

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

  def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T

  def interrupt(): Unit = interrupted = true

  def isInterrupted: Boolean = interrupted

}

object Debugger {

  import DebuggerActor.Message

  def apply[T](logger: Logger = Logger[Debugger], sequentially: Boolean = false)(f: Debugger => T): T = {
    val actor = if (sequentially) {
      new DebuggerSeq(logger)
    } else {
      val actor = new DebuggerActor(logger)
      new Thread(actor).start()
      actor
    }
    try {
      f(new ActorDebugger(logger, actor))
    } finally {
      actor ! Message.Stop
    }
  }

  implicit object EmptyDebugger extends Debugger {
    val logger: Logger = Logger[Debugger]

    def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T = f(EmptyActionDebugger)
  }

  class LoggerDebugger(val logger: Logger) extends Debugger {
    def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T = f(EmptyActionDebugger)
  }

  private class ActorDebugger(val logger: Logger, debugger: DebuggerMessageReceiver) extends Debugger {
    def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T = {
      val ad = new ActorActionDebugger(name, num, debugger)
      debugger ! Message.NewAction(name, num)
      try {
        f(ad)
      } finally {
        debugger ! Message.CloseAction(name)
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

  private class ActorActionDebugger(name: String, num: Int, debugger: DebuggerMessageReceiver) extends ActionDebugger {
    def done(msg: String = ""): Unit = debugger ! Message.Debug(name, msg)
  }

  private object EmptyActionDebugger extends ActionDebugger {
    def done(msg: String = ""): Unit = {}
  }

  private trait DebuggerMessageReceiver {
    protected val logger: Logger
    private val debugClock: Long = (5 seconds).toMillis
    private var currentAction: Option[Action] = None
    private var lastDump = System.currentTimeMillis()
    private var lastNum = 0

    private class Action(val name: String, maxNum: Int) {

      private var num = 0
      private var _state = ""

      def ++ : Action = {
        num += 1
        this
      }

      def state: String = _state

      def state_=(value: String): Unit = _state = value

      def absoluteProgress: Int = num

      def relativeProgress: Double = if (maxNum > 0) num.toDouble / maxNum else 0.0

      def hasProgressBar: Boolean = maxNum > 0

      override def toString: String = if (hasProgressBar) {
        s"Action $name, steps: $num of $maxNum, progress: ${(relativeProgress * 100).floor}%"
      } else {
        s"Action $name, steps: $num"
      }

    }

    private def dump(action: Action, msg: String, started: Boolean, ended: Boolean): Unit = {
      action.state = msg
      val isBorder = started || ended
      if (lastDump + debugClock < System.currentTimeMillis() || isBorder) {
        val rating = if (!isBorder) {
          val windowTime = System.currentTimeMillis() - lastDump
          val windowNum = action.absoluteProgress - lastNum
          s" (${round((windowNum.toDouble / windowTime) * 1000, 2)} per sec)"
        } else ""
        logger.info(action.toString + rating + (if (action.state.nonEmpty) " -- " + action.state else ""))
        lastDump = System.currentTimeMillis()
        lastNum = action.absoluteProgress
      }
    }

    protected def stop(): Unit

    protected def processMessage(message: Message): Unit = {
      message match {
        case Message.Debug(name, msg) =>
          for (action <- currentAction if action.name == name) {
            action.++
            dump(action, msg, false, false)
          }
        case Message.NewAction(name, num) =>
          if (currentAction.isEmpty) {
            val action = new Action(name, num)
            currentAction = Some(action)
            lastNum = 0
            dump(action, "started", true, false)
          }
        case Message.CloseAction(name) =>
          for (action <- currentAction if action.name == name) {
            dump(action, "ended", false, true)
            currentAction = None
          }
        case Message.Stop => stop()
      }
    }

    def !(message: Message): Unit
  }

  private class DebuggerActor(protected val logger: Logger) extends Runnable with DebuggerMessageReceiver {
    private val messages = new LinkedBlockingQueue[Message]
    private var stopped = false

    protected def stop(): Unit = {
      stopped = true
    }

    def run(): Unit = {
      while (!stopped) {
        processMessage(messages.take())
      }
    }

    def !(message: Message): Unit = messages.put(message)
  }

  private class DebuggerSeq(protected val logger: Logger) extends DebuggerMessageReceiver {
    protected def stop(): Unit = {}

    def !(message: Message): Unit = processMessage(message)
  }

  private object DebuggerActor {

    sealed trait Message

    object Message {

      case class NewAction(name: String, num: Int) extends Message

      case class Debug(name: String, msg: String = "") extends Message

      case class CloseAction(name: String) extends Message

      case object Stop extends Message

    }

  }

}