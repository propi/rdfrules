package com.github.propi.rdfrules.stringifier

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
trait Stringifier[T] {

  def toStringValue(v: T): String

}

object Stringifier {

  def apply[T](implicit stringifier: Stringifier[T]): Stringifier[T] = stringifier

  def apply[T](v: T)(implicit stringifier: Stringifier[T]): String = stringifier.toStringValue(v)

}
