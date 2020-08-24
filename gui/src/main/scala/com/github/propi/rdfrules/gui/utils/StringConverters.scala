package com.github.propi.rdfrules.gui.utils

import com.github.propi.rdfrules.gui.utils.Validate.ValidationException

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Try}

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object StringConverters {

  implicit class StringOps(x: String) {
    def toJsonArray: js.Array[js.Dynamic] = JSON.parse(x).asInstanceOf[js.Array[js.Dynamic]]

    def toJson: js.Dynamic = JSON.parse(x)
  }

  implicit def stringToBoolean(x: String): Boolean = Predef.augmentString(x).toBoolean

  implicit def stringToInt(x: String): Int = Predef.augmentString(x).toInt

  implicit def stringToDouble(x: String): Double = Predef.augmentString(x).toDouble

  implicit def stringToLong(x: String): Long = Predef.augmentString(x).toLong

  implicit def stringToTryInt(x: String): Try[Int] = Try(stringToInt(x)).recoverWith {
    case _ => Failure(ValidationException(s"The value '$x' can not be parsed as an integer value."))
  }

  implicit def stringToTryDouble(x: String): Try[Double] = Try(stringToDouble(x)).recoverWith {
    case _ => Failure(ValidationException(s"The value '$x' can not be parsed as a double value."))
  }

  implicit def stringToTryLong(x: String): Try[Long] = Try(stringToLong(x)).recoverWith {
    case _ => Failure(ValidationException(s"The value '$x' can not be parsed as a long value."))
  }

}
