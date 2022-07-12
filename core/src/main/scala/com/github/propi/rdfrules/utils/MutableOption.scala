package com.github.propi.rdfrules.utils

class MutableOption[T] private {
  private var value: T = _
  private var isEmpty: Boolean = true

  def set(x: T): this.type = {
    value = x
    isEmpty = false
    this
  }

  def unset(): this.type = {
    isEmpty = true
    this
  }

  def toOption: Option[T] = if (isEmpty) None else Some(value)
}

object MutableOption {
  def empty[T]: MutableOption[T] = new MutableOption[T]

  def apply[T](x: T): MutableOption[T] = empty[T].set(x)
}