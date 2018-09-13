package com.github.propi.rdfrules.gui.utils

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object StringConverters {

  implicit def stringToJsonArray(x: String): js.Array[js.Dynamic] = JSON.parse(x).asInstanceOf[js.Array[js.Dynamic]]

}
