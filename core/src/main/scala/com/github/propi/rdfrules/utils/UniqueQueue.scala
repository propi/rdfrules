package com.github.propi.rdfrules.utils

import java.util.concurrent.ConcurrentLinkedQueue

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 20. 6. 2017.
  */
trait UniqueQueue[T] {

  /**
    * Add item into the queue
    *
    * @param item item
    * @return true = item added, false = item is already placed in the queue
    */
  def add(item: T): Boolean

  def peek: T

  def poll: T

  def peekOption: Option[T]

  def pollOption: Option[T]

  def isEmpty: Boolean

  def size: Int

}

object UniqueQueue {

  class ConcurrentLinkedQueueWrapper[T](queue: ConcurrentLinkedQueue[T]) extends UniqueQueue[T] {
    def add(item: T): Boolean = queue.add(item)

    def peekOption: Option[T] = Option(queue.peek())

    def pollOption: Option[T] = Option(queue.poll())

    def peek: T = queue.peek()

    def poll: T = queue.poll()

    def isEmpty: Boolean = queue.isEmpty

    def size: Int = queue.size()
  }

  class ThreadSafeUniqueSet[T] extends UniqueQueue[T] {
    private val queue = collection.mutable.LinkedHashSet.empty[T]

    def add(item: T): Boolean = queue.synchronized {
      queue.add(item)
    }

    def peek: T = queue.synchronized(queue.head)

    def poll: T = queue.synchronized {
      val x = queue.head
      queue -= x
      x
    }

    def peekOption: Option[T] = queue.synchronized(queue.headOption)

    def pollOption: Option[T] = queue.synchronized {
      val x = queue.headOption
      x.foreach(queue -= _)
      x
    }

    def isEmpty: Boolean = queue.synchronized(queue.isEmpty)

    def size: Int = queue.synchronized(queue.size)
  }

  implicit def concurrentLinkedQueueToUniqueQueue[T](queue: ConcurrentLinkedQueue[T]): UniqueQueue[T] = new ConcurrentLinkedQueueWrapper[T](queue)

}