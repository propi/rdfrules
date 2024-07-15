package com.github.propi.rdfrules.index

trait IntervalsIndex {

  def isEmpty: Boolean

  def parent(x: Int): Option[Int]

}
