package com.github.propi.rdfrules.http.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.propi.rdfrules.http.InMemoryCache
import com.github.propi.rdfrules.http.formats.CacheJsonFormats._

/**
  * Created by Vaclav Zeman on 27. 1. 2020.
  */
class Cache {

  val route: Route = pathPrefix("cache") {
    pathEnd {
      complete(InMemoryCache.getMemoryInfo)
    } ~ path("clear") {
      InMemoryCache.clear()
      System.gc()
      complete("cleared")
    }
  }

}
