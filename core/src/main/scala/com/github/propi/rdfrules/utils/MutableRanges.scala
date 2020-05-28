package com.github.propi.rdfrules.utils

import java.util

/**
  * Created by Vaclav Zeman on 28. 5. 2020.
  */
class MutableRanges {
  private val ranges = new util.LinkedList[MutableRanges.Item]

  private def getLast: Option[MutableRanges.Item] = if (ranges.isEmpty) None else Some(ranges.getLast)

  private def getFirst: Option[MutableRanges.Item] = if (ranges.isEmpty) None else Some(ranges.getFirst)

  def +=(x: Int): Unit = {
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
  }

  def clear(): Unit = ranges.clear()

  def isInRange(x: Int): Boolean = getFirst match {
    case Some(MutableRanges.Item.Range(start, end)) =>
      if (x >= start) {
        if (x < end) {
          true
        } else {
          ranges.removeFirst()
          true
        }
      } else {
        false
      }
    case Some(MutableRanges.Item.Value(y)) =>
      if (x < y) {
        false
      } else {
        ranges.removeFirst()
        true
      }
    case None => false
  }
}

object MutableRanges {

  trait Item

  object Item {

    case class Value(value: Int) extends Item

    case class Range(start: Int, end: Int) extends Item

  }

}