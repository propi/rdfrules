package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.Prefix
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

/**
  * Created by Vaclav Zeman on 15. 8. 2018.
  */
object CommonDataJsonFormats {

  implicit val prefixFullFormat: RootJsonFormat[Prefix.Full] = jsonFormat2(Prefix.Full)

  implicit val prefixNamespaceFormat: RootJsonFormat[Prefix.Namespace] = jsonFormat1(Prefix.Namespace)

  implicit val prefixFormat: RootJsonFormat[Prefix] = new RootJsonFormat[Prefix] {
    def write(obj: Prefix): JsValue = obj match {
      case x: Prefix.Full => x.toJson
      case x: Prefix.Namespace => x.toJson
    }

    def read(json: JsValue): Prefix = Try(json.convertTo[Prefix.Full]).getOrElse(json.convertTo[Prefix.Namespace])
  }

}