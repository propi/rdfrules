package com.github.propi.rdfrules.http.util

import akka.http.scaladsl.server.Rejection
import spray.json.{DeserializationException, SerializationException}

/**
  * Created by Vaclav Zeman on 7. 8. 2017.
  */
object BasicExceptions {

  object DeserializationIsNotSupported extends DeserializationException("Deserialization is not supported.")

  object SerializationIsNotSupported extends SerializationException("Serialization is not supported.")

  case class ValidationException(code: String, msg: String) extends Exception(msg) with Rejection

  object ValidationException {
    def apply(msg: String): ValidationException = new ValidationException("Unspecified", msg)

    def apply(): ValidationException = apply("Unspecified exception.")

    val InvalidInputData = ValidationException("InvalidInputData", "Invalid input data.")
  }

}