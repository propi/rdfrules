package com.github.propi.rdfrules.java

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 10. 5. 2018.
  */
object ScalaConverters {

  def toIterable[JT, ST](iterable: java.lang.Iterable[JT], map: JT => ST): Iterable[ST] = iterable.asScala.view.map(map)

}