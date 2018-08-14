package com.github.propi.rdfrules.http.service

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import com.github.propi.rdfrules.http.actor
import com.github.propi.rdfrules.http.task.Pipeline
import com.github.propi.rdfrules.utils.Debugger
import spray.json.{JsValue, _}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 13. 8. 2018.
  */
class Task(implicit actorSystem: ActorSystem) {

  private implicit val timeout: Timeout = 30 seconds

  val route: Route = pathPrefix("task") {
    pathEnd {
      post {
        entity(as[Debugger => Pipeline[Source[JsValue, NotUsed]]]) { pipeline =>
          val id = UUID.randomUUID()
          actorSystem.actorOf(actor.Task.props(id, pipeline), "task-" + id.toString)
          complete(StatusCodes.Accepted)
        }
      }
    } ~ path(JavaUUID) { id =>
      get {
        onComplete(actorSystem.actorSelection(s"/user/task-${id.toString}").resolveOne) {
          case Success(task) => onSuccess(task ? actor.Task.Request.GetResult) {
            case x: actor.Task.Response.InProgress => complete(StatusCodes.Accepted, x)
            case x: actor.Task.Response.Result =>
              val header = x.toJson.compactPrint.replaceAll("\\}\\s*$", "")
              implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json().withFramingRenderer(Flow[ByteString].intersperse(
                ByteString(header + ", \"result\": ["), ByteString(","), ByteString("]}")
              ))
              complete(x.result)
            case _ => reject
          }
          case Failure(_) => reject
        }
      }
    }
  }

}
