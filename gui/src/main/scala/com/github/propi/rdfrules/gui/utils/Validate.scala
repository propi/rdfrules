package com.github.propi.rdfrules.gui.utils

import scala.util.{Failure, Success, Try}

/**
  * Created by Vaclav Zeman on 28. 1. 2019.
  */
object Validate {

  trait Validator[T] {
    def validate(x: T): Try[T]

    def map[A](implicit x: A => Try[T]): Validator[A] = CommonValidators.MappedValidator(this, x)

    final def &(validator: Validator[T]): Validator[T] = Comb(this, validator)
  }

  case class Comb[T](v1: Validator[T], v2: Validator[T]) extends Validator[T] {
    def validate(x: T): Try[T] = v1.validate(x).flatMap(v2.validate)
  }

  def apply[T](x: T)(implicit validator: Validator[T]): Try[T] = validator.validate(x)

  case class ValidationException(msg: String) extends Exception(msg)

  trait CommonValidator[T] extends Validator[T] {
    protected def isValid(x: T): Option[String]

    final def validate(x: T): Try[T] = isValid(x) match {
      case Some(errorMsg) => Failure(ValidationException(errorMsg))
      case None => Success(x)
    }
  }

  case class NoValidator[T]() extends Validator[T] {
    def validate(x: T): Try[T] = Success(x)
  }

  implicit class PimpedValidationResult[T](x: Try[T]) {
    def errorMsg: Option[String] = x.failed.map(_.getMessage).toOption
  }

}