package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 2. 1. 2018.
  */
sealed trait TripleItemPosition[+T] {
  val item: T

  def map[A](f: T => A): TripleItemPosition[A]
}

object TripleItemPosition {

  case class Subject[T](item: T) extends TripleItemPosition[T] {
    def map[A](f: T => A): TripleItemPosition[A] = Subject(f(item))
  }

  case class Object[T](item: T) extends TripleItemPosition[T] {
    def map[A](f: T => A): TripleItemPosition[A] = Object(f(item))
  }

}