package com.github.propi.rdfrules.http.service

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.github.propi.rdfrules.http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.github.propi.rdfrules.http.formats.WorkspaceJsonFormats._

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
class Workspace {

  val route: Route = pathPrefix("workspace") {
    pathEnd {
      get {
        complete(http.Workspace.getTree)
      }
    }
  }

}
