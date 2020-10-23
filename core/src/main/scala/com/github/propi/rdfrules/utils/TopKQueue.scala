package com.github.propi.rdfrules.utils

/**
  * Created by Vaclav Zeman on 26. 8. 2020.
  */
class TopKQueue[T](capacity: Int, allowOverflowIfSameScore: Boolean)(implicit ord: Ordering[T]) {

  private val queue = collection.mutable.PriorityQueue.empty[T]
  private var overflowBuffer: List[T] = Nil

  /**
    * Enqueue an element to the topK queue
    *
    * @param x the element
    * @return true = the element has been enqueued, false = the element has not been enqueued since it has too low score
    */
  def enqueue(x: T): Boolean = {
    if (queue.size < capacity) {
      queue.enqueue(x)
      true
    } else if (ord.lt(x, queue.head)) {
      val removed = queue.dequeue()
      queue.enqueue(x)
      if (allowOverflowIfSameScore) {
        if (ord.equiv(queue.head, removed)) {
          overflowBuffer = removed :: overflowBuffer
        } else {
          overflowBuffer = Nil
        }
      }
      true
    } else if (allowOverflowIfSameScore && ord.equiv(x, queue.head)) {
      overflowBuffer = x :: overflowBuffer
      true
    } else {
      false
    }
  }

  def dequeue: Option[T] = {
    if (overflowBuffer.nonEmpty) {
      val x = overflowBuffer.head
      overflowBuffer = overflowBuffer.tail
      Some(x)
    } else if (queue.nonEmpty) {
      Some(queue.dequeue())
    } else {
      None
    }
  }

  def head: Option[T] = overflowBuffer.headOption.orElse(queue.headOption)

  def isFull: Boolean = queue.length >= capacity

  def isOverflowed: Boolean = overflowBuffer.nonEmpty

  def dequeueAll: Iterator[T] = Iterator.continually(dequeue).takeWhile(_.isDefined).map(_.get)

}