package com.github.propi.rdfrules.gui.utils

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object StringConverters {

  implicit class StringOps(x: String) {
    def toJsonArray: js.Array[js.Dynamic] = JSON.parse(x).asInstanceOf[js.Array[js.Dynamic]]
  }

  implicit def stringToInt(x: String): Int = Predef.augmentString(x).toInt

  implicit def stringToDouble(x: String): Double = Predef.augmentString(x).toDouble

}
