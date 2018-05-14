package com.github.propi.rdfrules.java

import java.util.Comparator

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
class OrderingWrapper[T](comparator: Comparator[T]) extends Ordering[T] {
  def compare(x: T, y: T): Int = comparator.compare(x, y)
}