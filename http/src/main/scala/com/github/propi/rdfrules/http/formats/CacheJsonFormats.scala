package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.http.InMemoryCache.MemoryInfo
import spray.json.{RootJsonFormat, _}

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object CacheJsonFormats extends DefaultJsonProtocol {

  implicit val memoryInfoFormat: RootJsonFormat[MemoryInfo] = jsonFormat2(MemoryInfo)

}