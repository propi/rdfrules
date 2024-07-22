package com.github.propi.rdfrules.index

import scala.language.implicitConversions

trait IntervalsIndex {

  def isEmpty: Boolean

  def parent(x: Int): Option[Int]

  def iterator: Iterator[(Int, Int)]

}

object IntervalsIndex {

  implicit object Empty extends IntervalsIndex {
    def isEmpty: Boolean = true

    def parent(x: Int): Option[Int] = None

    def iterator: Iterator[(Int, Int)] = Iterator.empty
  }

}