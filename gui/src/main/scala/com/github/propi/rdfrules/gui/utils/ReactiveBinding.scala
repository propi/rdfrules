package com.github.propi.rdfrules.gui.utils

import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.BindingSeq

import scala.collection.mutable

/**
  * Created by Vaclav Zeman on 29. 1. 2019.
  */
object ReactiveBinding {

  trait Listener[T] {
    def changed(oldValue: T, newValue: T): Unit
  }

  trait SeqListener[T] {
    def changed(newCol: Seq[T]): Unit = {}

    def updated(oldValue: T, newValue: T, position: Int): Unit = {}

    def added(value: T, appended: Boolean, newLength: Int): Unit = {}

    def removed(value: T, position: Int): Unit = {}

    def cleared(): Unit = {}
  }

  class Var[T] private(x: T) {
    private val bindingVar: Binding.Var[T] = Binding.Var(x)
    private val buffer: mutable.ListBuffer[Listener[T]] = mutable.ListBuffer.empty

    def addListener(listener: Listener[T]): Unit = buffer += listener

    def value: T = bindingVar.value

    def value_=(newValue: T): Unit = {
      val x = value
      if (x != newValue) {
        bindingVar.value = newValue
        buffer.foreach(_.changed(x, newValue))
      }
    }

    def binding: Binding[T] = bindingVar
  }

  object Var {
    def apply[T](x: T): Var[T] = new Var(x)
  }

  class Vars[T] private {
    private val bindingVars: Binding.Vars[T] = Binding.Vars()
    private val listeners: mutable.ListBuffer[SeqListener[T]] = mutable.ListBuffer.empty

    def addListener(listener: SeqListener[T]): Unit = listeners += listener

    def value: mutable.Buffer[T] = new Proxy(bindingVars.value)

    def binding: BindingSeq[T] = bindingVars

    private final class Proxy(buffer: mutable.Buffer[T]) extends mutable.Buffer[T] {
      def apply(n: Int): T = buffer(n)

      def update(n: Int, newelem: T): Unit = {
        val oldValue = apply(n)
        if (oldValue != newelem) {
          buffer.update(n, newelem)
          for (listener <- listeners) {
            listener.updated(oldValue, newelem, n)
            listener.changed(this)
          }
        }
      }

      def length: Int = buffer.length

      def +=(elem: T): Proxy.this.type = {
        buffer += elem
        for (listener <- listeners) {
          listener.added(elem, true, length)
          listener.changed(this)
        }
        this
      }

      def clear(): Unit = {
        buffer.clear()
        for (listener <- listeners) {
          listener.cleared()
          listener.changed(this)
        }
      }

      def +=:(elem: T): Proxy.this.type = {
        elem +=: buffer
        for (listener <- listeners) {
          listener.added(elem, false, length)
          listener.changed(this)
        }
        this
      }

      override def ++=(elements: TraversableOnce[T]): Proxy.this.type = {
        val oldLength = length
        buffer ++= elements
        for {
          i <- oldLength until length
          listener <- listeners
        } {
          listener.added(apply(i), true, i + 1)
        }
        for (listener <- listeners) {
          listener.changed(this)
        }
        this
      }

      def insertAll(n: Int, elems: Traversable[T]): Unit = {
        val elemsArray = elems.toVector
        val oldArray = (for (i <- n until (n + elemsArray.length)) yield apply(i)).toVector
        buffer.insertAll(n, elemsArray)
        for {
          i <- elemsArray.indices
          listener <- listeners
        } {
          listener.updated(oldArray(i), elemsArray(i), n + i)
        }
        for (listener <- listeners) {
          listener.changed(this)
        }
      }

      def remove(n: Int): T = {
        val oldValue = buffer.remove(n)
        for (listener <- listeners) {
          listener.removed(oldValue, n)
          listener.changed(this)
        }
        oldValue
      }

      def iterator: Iterator[T] = buffer.iterator
    }

  }

  object Vars {
    def apply[T](elems: T*): Vars[T] = {
      val vars = new Vars[T]()
      vars.bindingVars.value ++= elems
      vars
    }
  }

}