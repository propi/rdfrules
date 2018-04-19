package com.github.propi.rdfrules.utils

import java.util.concurrent.LinkedBlockingQueue

import com.github.propi.rdfrules.utils.Debugger.ActionDebugger
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait Debugger {

  def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T

}

object Debugger {

  import DebuggerActor.Message

  def apply[T](logger: Logger = Logger[Debugger])(f: Debugger => T): T = {
    val actor = new DebuggerActor(logger)
    new Thread(actor).start()
    try {
      f(new ActorDebugger(actor))
    } finally {
      actor ! Message.Stop
    }
  }

  object EmptyDebugger extends Debugger {
    def debug[T](name: String, num: Int = 0)(f: (ActionDebugger) => T): T = f(EmptyActionDebugger)
  }

  private class ActorDebugger(debugger: DebuggerActor) extends Debugger {

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

  private class ActorActionDebugger(name: String, num: Int, debugger: DebuggerActor) extends ActionDebugger {
    def done(msg: String = ""): Unit = debugger ! Message.Debug(name, msg)
  }

  private object EmptyActionDebugger extends ActionDebugger {
    def done(msg: String = ""): Unit = {}
  }

  private class DebuggerActor(logger: Logger) extends Runnable {

    private val messages = new LinkedBlockingQueue[Message]
    private val debugClock: FiniteDuration = 5 seconds
    private val actions = collection.mutable.Map.empty[String, Action]
    private var lastDump = 0L

    class Action(name: String, maxNum: Int) {

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
        s"Action $name, steps: $num from $maxNum, progress: ${(relativeProgress * 100).round}%"
      } else {
        s"Action $name, steps: $num"
      }

    }

    private def dump(action: Action, msg: String): Unit = {
      action.state = msg
      if (lastDump + debugClock.toMillis < System.currentTimeMillis()) {
        for (action <- actions.valuesIterator) {
          logger.info(action.toString + (if (action.state.nonEmpty) "\n" + " - current state: " + action.state else ""))
        }
        lastDump = System.currentTimeMillis()
      }
    }

    def run(): Unit = {
      var stopped = false
      while (!stopped) {
        messages.take() match {
          case Message.NewAction(name, num) =>
            val action = new Action(name, num)
            actions += (name -> action)
            dump(action, "started")
          case Message.Debug(name, msg) => actions.get(name).foreach { action =>
            action.++
            dump(action, msg)
          }
          case Message.CloseAction(name) => actions.get(name).foreach { action =>
            dump(action, "ended")
            actions -= name
          }
          case Message.Stop => stopped = true
        }
      }
    }

    def !(message: Message): Unit = messages.put(message)

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