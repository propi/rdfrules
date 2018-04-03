package com.github.propi.rdfrules.utils

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
class IncrementalInt {

  private var value = 0

  def ++ : IncrementalInt = this += 1

  def +=(x: Int): IncrementalInt = {
    value += x
    this
  }

  def getValue: Int = value

}

object IncrementalInt {

  def apply(): IncrementalInt = new IncrementalInt

  def apply(value: Int): IncrementalInt = apply += value

}
