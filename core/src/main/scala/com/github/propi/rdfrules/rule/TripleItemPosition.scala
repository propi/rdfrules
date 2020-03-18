package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TripleItemPosition[+T] {
  val item: T
}

object TripleItemPosition {

  case class Subject[T](item: T) extends TripleItemPosition[T]

  case class Object[T](item: T) extends TripleItemPosition[T]

}