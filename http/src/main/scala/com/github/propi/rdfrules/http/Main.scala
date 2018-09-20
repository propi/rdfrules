package com.github.propi.rdfrules.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import com.github.propi.rdfrules.http.util.{DefaultServer, DefaultServerConf}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object Main extends DefaultServer with DefaultServerConf {

  val confPrefix = "rdfrules"
  val configServerPrefix: String = s"$confPrefix.server"

  implicit val actorSystem: ActorSystem = ActorSystem("rdfrules-http")
  implicit val materializer: Materializer = ActorMaterializer()

  val route: Route = (new service.Workspace).route ~ (new service.Task).route ~ webappDir.map { dir =>
    pathPrefix("webapp") {
      pathEndOrSingleSlash {
        extractUri { uri =>
          redirect(uri.withPath(uri.path ?/ "index.html"), StatusCodes.SeeOther)
        }
      } ~ getFromBrowseableDirectory(dir)
    }
  }.getOrElse(reject)

  def main(args: Array[String]): Unit = {
    Await.result(bind(), 30 seconds)
    if (stoppingToken.trim.isEmpty) {
      println("Press enter to exit: ")
      StdIn.readLine()
      Await.result(stop(), 30 seconds)
    }
  }

}