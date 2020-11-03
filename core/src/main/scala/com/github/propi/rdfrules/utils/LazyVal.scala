package com.github.propi.rdfrules.utils

import scala.language.implicitConversions

class LazyVal[T](f: => T) {

  @volatile private var _isEvaluated = false

  private var _value: T = _

  def isDefined: Boolean = _isEvaluated

  def apply(): T = {
    if (!_isEvaluated) {
      this.synchronized {
        _value = f
        _isEvaluated = true
      }
    }
    _value
  }

}

object LazyVal {

  implicit def lazyValToValue[T](value: LazyVal[T]): T = value()

  def apply[T](builder: => T): LazyVal[T] = new LazyVal[T](builder)

}