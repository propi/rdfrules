package com.github.propi.rdfrules.http.service

import java.util.{Date, UUID}

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import com.github.propi.rdfrules.http.formats.CommonDataJsonWriters._
import com.github.propi.rdfrules.http.formats.PipelineJsonReaders._
import com.github.propi.rdfrules.http.service.Task.{ReturnedTask, TaskRequest, TaskResponse, TaskServiceRequest}
import com.github.propi.rdfrules.http.task.Pipeline
import com.github.propi.rdfrules.utils.{CustomLogger, Debugger}
import spray.json.{JsValue, _}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
class Task(taskService: ActorRef[TaskServiceRequest])(implicit system: Scheduler) {

  private implicit val timeout: Timeout = 30 seconds

  val route: Route = pathPrefix("task") {
    pathEnd {
      post {
        extractUri { uri =>
          entity(as[Debugger => Pipeline[Source[JsValue, NotUsed]]]) { pipeline =>
            val id = UUID.randomUUID()
            val started = new Date
            taskService ! TaskServiceRequest.CreateTask(id, started, pipeline)
            complete(StatusCodes.Accepted, List(Location(uri.withPath(uri.path / id.toString))), TaskResponse.InProgress(id, started, Nil))
          }
        }
      }
    } ~ path(JavaUUID) { id =>
      onComplete(taskService.ask[ReturnedTask](sender => TaskServiceRequest.GetTask(id, sender))) {
        case Success(ReturnedTask(Some(task))) => get {
          onSuccess(task.ask[TaskResponse](TaskRequest.GetResult)) {
            case x: TaskResponse.InProgress => complete(StatusCodes.Accepted, x)
            case x: TaskResponse.Result =>
              val header = x.toJson.compactPrint.replaceAll("\\}\\s*$", "")
              implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json().withFramingRenderer(Flow[ByteString].intersperse(
                ByteString(header + ", \"result\": ["), ByteString(","), ByteString("]}")
              ))
              complete(ToResponseMarshallable.apply(x.result))
            case TaskResponse.Failure(th) => failWith(th)
          }
        } ~ delete {
          task ! TaskRequest.Interrupt
          complete(StatusCodes.Accepted, "accepted")
        }
        case Success(ReturnedTask(None)) => reject
        case Failure(th) => failWith(th)
      }
    }
  }

}

object Task {

  sealed trait TaskServiceRequest

  object TaskServiceRequest {

    case class CreateTask(id: UUID, started: Date, pipeline: Debugger => Pipeline[Source[JsValue, NotUsed]]) extends TaskServiceRequest

    case class GetTask(id: UUID, sender: ActorRef[ReturnedTask]) extends TaskServiceRequest

  }

  case class ReturnedTask(task: Option[ActorRef[TaskRequest]])

  sealed trait TaskRequest

  object TaskRequest {

    case class GetResult(sender: ActorRef[TaskResponse]) extends TaskRequest

    case class AddMsg(msg: String) extends TaskRequest

    case object Cancel extends TaskRequest

    case object Interrupt extends TaskRequest

    case class RegisterDebugger(debugger: Debugger) extends TaskRequest

    case object UnregisterDebugger extends TaskRequest

  }

  sealed trait TaskResponse

  object TaskResponse {

    case class InProgress(id: UUID, started: Date, msg: Seq[(String, Date)]) extends TaskResponse

    case class Failure(th: Throwable) extends TaskResponse

    case class Result(id: UUID, started: Date, finished: Date, msg: Seq[(String, Date)], result: Source[JsValue, NotUsed]) extends TaskResponse

  }

  private def createTask(id: UUID, started: Date, pipeline: Debugger => Pipeline[Source[JsValue, NotUsed]]): Behavior[TaskRequest] = Behaviors.setup { context =>
    context.setReceiveTimeout(10 minutes, TaskRequest.Cancel)
    implicit val ec: ExecutionContext = context.executionContext
    val result = Future {
      val logger = CustomLogger("task-" + id.toString) { (msg, level) =>
        if (level.toInt >= 20) context.self ! TaskRequest.AddMsg(msg)
      }
      val result = Debugger(logger) { debugger =>
        context.self ! TaskRequest.RegisterDebugger(debugger)
        try {
          pipeline(debugger).execute
        } finally {
          context.self ! TaskRequest.UnregisterDebugger
        }
      }
      new Date() -> result
    }

    def changeTask(log: Vector[(String, Date)], debugger: Option[Debugger]): Behavior[TaskRequest] = Behaviors.receiveMessage {
      case TaskRequest.GetResult(sender) => result.value match {
        case Some(Success((finished, result))) =>
          sender ! TaskResponse.Result(id, started, finished, log, result)
          Behaviors.stopped
        case Some(Failure(th)) =>
          sender ! TaskResponse.Failure(th)
          Behaviors.stopped
        case None =>
          sender ! TaskResponse.InProgress(id, started, log)
          Behaviors.same
      }
      case TaskRequest.RegisterDebugger(debugger) =>
        changeTask(log, Some(debugger))
      case TaskRequest.UnregisterDebugger =>
        changeTask(log, None)
      case TaskRequest.AddMsg(msg) =>
        changeTask(log :+ (msg -> new Date()), debugger)
      case TaskRequest.Interrupt =>
        debugger.foreach(_.interrupt())
        Behaviors.same
      case TaskRequest.Cancel =>
        debugger.foreach(_.interrupt())
        Behaviors.stopped
    }

    changeTask(Vector.empty, None)
  }

  def taskFactoryActor: Behavior[TaskServiceRequest] = Behaviors.receive {
    case (context, TaskServiceRequest.CreateTask(id, started, pipeline)) =>
      context.spawn(createTask(id, started, pipeline), "task-" + id.toString)
      Behaviors.same
    case (context, TaskServiceRequest.GetTask(id, sender)) =>
      sender ! ReturnedTask(context.child("task-" + id.toString).map(_.unsafeUpcast[TaskRequest]))
      Behaviors.same
  }

}