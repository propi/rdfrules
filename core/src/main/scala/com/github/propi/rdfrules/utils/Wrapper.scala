package com.github.propi.rdfrules.utils

class Wrapper[T](val x: T)

object Wrapper {
  def apply[T](x: T): Wrapper[T] = new Wrapper(x)
}