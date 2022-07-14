package com.github.propi.rdfrules.utils

import com.github.propi.rdfrules.utils.Circle.LinkedItem

class Circle[T] private {
  private var linkedItem: LinkedItem[T] = new LinkedItem.Empty[T]

  def next(): this.type = {
    linkedItem = linkedItem.next
    this
  }

  def clear(): this.type = {
    linkedItem = new LinkedItem.Empty[T]
    this
  }

  def +=(value: T): this.type = {
    linkedItem = linkedItem.addOne(value)
    this
  }

  def remove(): this.type = {
    linkedItem = linkedItem.remove()
    this
  }

  def value: T = linkedItem.value

  def isEmpty: Boolean = linkedItem.isEmpty
}

object Circle {

  private sealed trait LinkedItem[T] {
    def next: LinkedItem[T]

    def value: T

    def addOne(value: T): LinkedItem[T]

    def remove(): LinkedItem[T]

    def isEmpty: Boolean
  }

  private object LinkedItem {

    private[Circle] class Empty[T] extends LinkedItem[T] {
      def next: LinkedItem[T] = this

      def value: T = throw new NoSuchElementException()

      def addOne(value: T): LinkedItem[T] = new Single(value)

      def remove(): LinkedItem[T] = this

      def isEmpty: Boolean = true
    }

    private class Single[T](val value: T) extends LinkedItem[T] {
      def next: LinkedItem[T] = this

      def addOne(value: T): LinkedItem[T] = {
        val multi1 = new Multi(this.value)
        val multi2 = new Multi(value)
        multi1._next = multi2
        multi1._prev = multi2
        multi2._next = multi1
        multi2._prev = multi1
        multi1
      }

      def remove(): LinkedItem[T] = new Empty[T]

      def isEmpty: Boolean = false
    }

    private class Multi[T](val value: T) extends LinkedItem[T] {
      private[LinkedItem] var _next: Multi[T] = _
      private[LinkedItem] var _prev: Multi[T] = _

      def next: LinkedItem[T] = _next

      def addOne(value: T): LinkedItem[T] = {
        val multi = new Multi(value)
        val __next = _next
        __next._prev = multi
        _next = multi
        multi._next = __next
        multi._prev = this
        this
      }

      def remove(): LinkedItem[T] = {
        if (_next == _prev) {
          new Single(_next.value)
        } else {
          _prev._next = _next
          _next._prev = _prev
          _next
        }
      }

      def isEmpty: Boolean = false
    }

  }

  def empty[T]: Circle[T] = new Circle[T]

  def apply[T](values: IterableOnce[T]): Circle[T] = {
    val circle = empty[T]
    values.iterator.foreach(circle += _)
    circle
  }

}


