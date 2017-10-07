package eu.easyminer.rdf.utils

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.utils.Debugger.ActionDebugger

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
trait Debugger {

  def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T

}

object Debugger {

  val logger: Logger = Logger[Debugger]

  def apply()(implicit af: ActorRefFactory): Debugger = new ActorDebugger(af.actorOf(DebuggerActor.props))

  object EmptyDebugger extends Debugger {
    def debug[T](name: String, num: Int = 0)(f: (ActionDebugger) => T): T = f(EmptyActionDebugger)
  }

  private class ActorDebugger(debugger: ActorRef) extends Debugger {

    def debug[T](name: String, num: Int = 0)(f: ActionDebugger => T): T = {
      val ad = new ActorActionDebugger(name, num, debugger)
      debugger ! Messages.NewAction(name, num)
      try {
        f(ad)
      } finally {
        debugger ! Messages.CloseAction(name)
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

  private class ActorActionDebugger(name: String, num: Int, debugger: ActorRef) extends ActionDebugger {
    def done(msg: String = ""): Unit = debugger ! Messages.Debug(name, msg)
  }

  private object EmptyActionDebugger extends ActionDebugger {
    def done(msg: String = ""): Unit = {}
  }

  private class DebuggerActor extends Actor {

    val debugClock: FiniteDuration = 5 seconds
    val actions = collection.mutable.Map.empty[String, Action]
    var lastDump = 0L

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

    def dump(action: Action, msg: String): Unit = {
      action.state = msg
      if (lastDump + debugClock.toMillis < System.currentTimeMillis()) {
        for (action <- actions.valuesIterator) {
          logger.info(action.toString + (if (action.state.nonEmpty) "\n" + " - current state: " + action.state else ""))
        }
        lastDump = System.currentTimeMillis()
      }
    }

    def receive: Receive = {
      case Messages.NewAction(name, num) =>
        val action = new Action(name, num)
        actions += (name -> action)
        dump(action, "started")
      case Messages.Debug(name, msg) => actions.get(name).foreach { action =>
        action.++
        dump(action, msg)
      }
      case Messages.CloseAction(name) => actions.get(name).foreach { action =>
        dump(action, "ended")
        actions -= name
      }
    }

  }

  private object DebuggerActor {

    def props = Props(new DebuggerActor)

  }

  private object Messages {

    case class NewAction(name: String, num: Int)

    case class Debug(name: String, msg: String = "")

    case class CloseAction(name: String)

  }

}