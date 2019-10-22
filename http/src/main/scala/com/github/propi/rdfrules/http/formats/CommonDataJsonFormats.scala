package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.data.Prefix
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Vaclav Zeman on 15. 8. 2018.
  */
object CommonDataJsonFormats {

  implicit val prefixFormat: RootJsonFormat[Prefix] = jsonFormat2(Prefix.apply)

}