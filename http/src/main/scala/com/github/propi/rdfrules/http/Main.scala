package com.github.propi.rdfrules.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, DispatcherSelector, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.propi.rdfrules.http.service.Task
import com.github.propi.rdfrules.http.util.Server.MainMessage
import com.github.propi.rdfrules.http.util.{Server, ServerConf}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object Main extends ServerConf {

  val confPrefix = "rdfrules"
  lazy val configServerPrefix = s"$confPrefix.server"

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[MainMessage] = ActorSystem(Behaviors.setup[MainMessage] { context =>
      implicit val scheduler: Scheduler = context.system.scheduler
      val taskService = context.spawn(Task.taskFactoryActor, "task-service")
      context.spawn(Workspace.lifetimeActor, "workspace-lifetime")
      context.spawn(InMemoryCache.autoCleaningActor, "inmemorycache-autocleaning")
      implicit val ec: ExecutionContext = context.system.dispatchers.lookup(DispatcherSelector.fromConfig("task-dispatcher"))
      val route: Route = mapResponseHeaders { headers =>
        val memoryInfo = InMemoryCache.getMemoryInfo
        headers ++ List(RawHeader("MemoryCache-Total", memoryInfo.total.toString), RawHeader("MemoryCache-Free", memoryInfo.free.toString), RawHeader("MemoryCache-Items", memoryInfo.itemsInCache.toString))
      } {
        (new service.Workspace).route ~ (new service.Cache).route ~ new Task(taskService).route ~ webappDir.map { dir =>
          pathPrefix("webapp") {
            pathEndOrSingleSlash {
              extractUri { uri =>
                redirect(uri.withPath(uri.path ?/ "index.html"), StatusCodes.SeeOther)
              }
            } ~ getFromBrowseableDirectory(dir)
          }
        }.getOrElse(reject)
      }
      val server = Server(context, route, this)
      server.bind()
      Behaviors.receiveMessagePartial[MainMessage] {
        case MainMessage.Stop =>
          server.stop()
          Behaviors.same
      }
    }, "rdfrules-http")
    if (stoppingToken.trim.isEmpty) {
      println("Press enter to exit: ")
      StdIn.readLine()
      system ! MainMessage.Stop
    }
    Await.result(system.whenTerminated, Duration.Inf)
    println("RDFRules http server finished.")
  }

}