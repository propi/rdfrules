package com.github.propi.rdfrules.gui.utils

import com.github.propi.rdfrules.gui.utils.Validate.{CommonValidator, Validator}

import scala.util.Try

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 29. 8. 2018.
  */
object CommonValidators {

  case object NonEmpty extends CommonValidator[String] {
    protected def isValid(x: String): Option[String] = if (x.trim.isEmpty) Some("Value must not be empty.") else None
  }

  case class GreaterThan[T](value: T)(implicit n: Numeric[T]) extends CommonValidator[T] {
    protected def isValid(x: T): Option[String] = if (n.gt(x, value)) None else Some(s"Value $x is not greater than $value")
  }

  case class GreaterThanOrEqualsTo[T](value: T)(implicit n: Numeric[T]) extends CommonValidator[T] {
    protected def isValid(x: T): Option[String] = if (n.gteq(x, value)) None else Some(s"Value $x is not greater than or equal to $value")
  }

  case class LowerThan[T](value: T)(implicit n: Numeric[T]) extends CommonValidator[T] {
    protected def isValid(x: T): Option[String] = if (n.lt(x, value)) None else Some(s"Value $x is not lower than $value")
  }

  case class LowerThanOrEqualsTo[T](value: T)(implicit n: Numeric[T]) extends CommonValidator[T] {
    protected def isValid(x: T): Option[String] = if (n.lteq(x, value)) None else Some(s"Value $x is not lower than or equal to $value")
  }

  case class MaxLength(length: Int) extends CommonValidator[String] {
    protected def isValid(x: String): Option[String] = if (x.length <= length) None else Some(s"Max value length is $length characters (currently: ${x.length}).")
  }

  case class RegExp(regexp: String, canBeEmpty: Boolean = false) extends CommonValidator[String] {
    protected def isValid(x: String): Option[String] = if ((x.isEmpty && canBeEmpty) || x.matches(regexp)) None else Some(s"Value does not match the pattern '$regexp'.")
  }

  case class MappedValidator[A, B](validator: Validator[A], f: B => Try[A]) extends Validator[B] {
    def validate(x: B): Try[B] = f(x).flatMap(validator.validate).map(_ => x)
  }

  implicit def mappedValidator[A, B](validator: Validator[A])(implicit f: B => Try[A]): Validator[B] = MappedValidator(validator, f)

}