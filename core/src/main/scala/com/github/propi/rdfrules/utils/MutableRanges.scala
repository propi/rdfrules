package com.github.propi.rdfrules.utils

import java.util
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
  * Created by Vaclav Zeman on 28. 5. 2020.
  */
class MutableRanges private(ranges: util.LinkedList[MutableRanges.Item], var i: Int) {
  private def getLast: Option[MutableRanges.Item] = if (ranges.isEmpty) None else Some(ranges.getLast)

  def size: Int = i

  def copy(): MutableRanges = new MutableRanges(ranges.clone().asInstanceOf[util.LinkedList[MutableRanges.Item]], i)

  def validator: MutableRanges.Validator = new MutableRanges.Validator(ranges.iterator().asScala)

  def +=(x: Int): this.type = {
    ranges.iterator()
    i += 1
    getLast match {
      case Some(MutableRanges.Item.Value(y)) =>
        if (y + 1 == x) {
          ranges.removeLast()
          ranges.add(MutableRanges.Item.Range(y, x))
        } else {
          ranges.add(MutableRanges.Item.Value(x))
        }
      case Some(MutableRanges.Item.Range(start, end)) =>
        if (end + 1 == x) {
          ranges.removeLast()
          ranges.add(MutableRanges.Item.Range(start, x))
        } else {
          ranges.add(MutableRanges.Item.Value(x))
        }
      case None => ranges.add(MutableRanges.Item.Value(x))
    }
    this
  }

  def clear(): Unit = ranges.clear()
}

object MutableRanges {

  class Validator private[MutableRanges](it: Iterator[MutableRanges.Item]) {
    private var _current = Option.empty[MutableRanges.Item]

    private def next(): Unit = if (it.hasNext) _current = Some(it.next()) else _current = None

    next()

    @tailrec
    final def isInRange(x: Int): Boolean = _current match {
      case Some(MutableRanges.Item.Range(start, end)) =>
        if (x >= start) {
          if (x <= end) {
            true
          } else {
            next()
            isInRange(x)
          }
        } else {
          false
        }
      case Some(MutableRanges.Item.Value(y)) =>
        if (x < y) {
          false
        } else if (x == y) {
          true
        } else {
          next()
          isInRange(x)
        }
      case None => false
    }
  }

  sealed trait Item

  object Item {

    case class Value(value: Int) extends Item

    case class Range(start: Int, end: Int) extends Item

  }

  def apply(): MutableRanges = new MutableRanges(new util.LinkedList[MutableRanges.Item], 0)

  /**
    * Make mutable ranges from 0 to x included
    *
    * @param x to included
    * @return
    */
  def apply(x: Int): MutableRanges = {
    val ranges = new util.LinkedList[MutableRanges.Item]
    if (x == 0) {
      ranges.add(Item.Value(0))
      new MutableRanges(ranges, 1)
    } else if (x > 0) {
      ranges.add(Item.Range(0, x))
      new MutableRanges(ranges, x + 1)
    } else {
      new MutableRanges(ranges, 0)
    }
  }

}