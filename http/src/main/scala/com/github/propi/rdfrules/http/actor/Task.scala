package com.github.propi.rdfrules.http.actor

import java.util.{Date, UUID}

import akka.NotUsed
import akka.actor.{Actor, Props, ReceiveTimeout, Status}
import akka.stream.scaladsl.Source
import com.github.propi.rdfrules.http.task.Pipeline
import com.github.propi.rdfrules.utils.{CustomLogger, Debugger}
import spray.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
class Task private(id: UUID, started: Date, pipeline: Debugger => Pipeline[Source[JsValue, NotUsed]]) extends Actor {

  context.setReceiveTimeout(10 minutes)

  private implicit val ec: ExecutionContext = context.dispatcher

  private val logBuffer = collection.mutable.ListBuffer.empty[(String, Date)]

  private val result = Future {
    val logger = CustomLogger("task-" + id.toString) { (msg, level) =>
      if (level.toInt >= 20) self ! Task.Request.AddMsg(msg)
    }
    val result = Debugger(logger) { debugger =>
       pipeline(debugger).execute
    }
    new Date() -> result
  }

  def receive: Receive = {
    case Task.Request.GetResult => result.value match {
      case Some(Success((finished, result))) =>
        sender() ! Task.Response.Result(id, started, finished, logBuffer.toList, result)
        context stop self
      case Some(Failure(th)) =>
        sender() ! Status.Failure(th)
        context stop self
      case None =>
        sender() ! Task.Response.InProgress(id, started, logBuffer.toList)
    }
    case Task.Request.AddMsg(msg) =>
      logBuffer += (msg -> new Date())
    case ReceiveTimeout =>
      context stop self
  }

  override def postStop(): Unit = logBuffer.clear()

}

object Task {

  def props(id: UUID, started: Date, pipeline: Debugger => Pipeline[Source[JsValue, NotUsed]]): Props = Props(new Task(id, started, pipeline))

  sealed trait Request

  object Request {

    case object GetResult extends Request

    case class AddMsg(msg: String) extends Request

  }

  sealed trait Response

  object Response {

    case class InProgress(id: UUID, started: Date, msg: List[(String, Date)])

    case class Result(id: UUID, started: Date, finished: Date, msg: List[(String, Date)], result: Source[JsValue, NotUsed])

  }

}