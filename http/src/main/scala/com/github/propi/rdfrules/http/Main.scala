package com.github.propi.rdfrules.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, DispatcherSelector, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.github.propi.rdfrules.http.formats.PipelineJsonReaders.pipelineReader
import com.github.propi.rdfrules.http.service.Task
import com.github.propi.rdfrules.http.task.Pipeline
import com.github.propi.rdfrules.http.util.Server.MainMessage
import com.github.propi.rdfrules.http.util.{Server, ServerConf}
import com.github.propi.rdfrules.utils.Debugger
import spray.json._

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.StdIn
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object Main extends ServerConf {

  val confPrefix = "rdfrules"
  lazy val configServerPrefix = s"$confPrefix.server"

  private def runHttp(): Unit = {
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
        pathPrefix("api") {
          (new service.Workspace).route ~ (new service.Cache).route ~ new Task(taskService).route
        } ~ webappDir.map { dir =>
          pathEndOrSingleSlash {
            extractUri { uri =>
              redirect(uri.withPath(uri.path ?/ "index.html"), StatusCodes.SeeOther)
            }
          } ~ getFromBrowseableDirectory(dir)
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

  private def runTask(path: String, target: Option[String]): Unit = {
    val file = new File(path)
    if (!file.isFile) throw new IllegalArgumentException(s"Path '$path' does not target to a valid task file.")
    ActorSystem(Behaviors.setup[Done] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = context.executionContext
      Debugger() { debugger =>
        val pipeline = Files.readString(file.toPath, StandardCharsets.UTF_8).parseJson.convertTo[Debugger => Pipeline[Source[JsValue, NotUsed]]]
        val writer = target.map(new File(_)).map(new PrintWriter(_, "UTF-8")).getOrElse(new PrintWriter(System.out, true))
        pipeline(debugger).execute.runForeach(x => writer.println(x.prettyPrint)).onComplete { res =>
          res.failed.foreach(_.printStackTrace())
          writer.close()
          context.self ! Done
        }
      }
      Behaviors.receiveMessage(_ => Behaviors.stopped)
    }, "BatchTask")
  }

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      runTask(args(0), args.lift(1))
    } else {
      runHttp()
    }
  }

}