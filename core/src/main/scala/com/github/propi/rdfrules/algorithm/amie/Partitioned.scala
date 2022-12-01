package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.Partitioned.{Part1, Part2}

import scala.reflect.ClassTag

trait Partitioned[T] {
  def part1Length: Int

  def part2Length: Int

  def part1Foreach(f: T => Unit): Unit

  def part2Foreach(f: T => Unit): Unit

  def part1Partition(f: T => Boolean): Partitioned[T]

  def part2Partition(f: T => Boolean): Partitioned[T]

  def remove(items: Array[T]): Partitioned[T]

  final def part1: Part1[T] = new Part1(this)

  final def part2: Part2[T] = new Part2(this)
}

object Partitioned {

  class Part1[T](val partitioned: Partitioned[T]) extends AnyVal {
    final def isEmpty: Boolean = partitioned.part1Length == 0

    final def nonEmpty: Boolean = !isEmpty

    final def partition(f: T => Boolean): (Part1[T], Part2[T]) = {
      val partitions = partitioned.part1Partition(f)
      partitions.part1 -> partitions.part2
    }

    final def foreach(f: T => Unit): Unit = partitioned.part1Foreach(f)
  }

  class Part2[T](val partitioned: Partitioned[T]) extends AnyVal {
    final def isEmpty: Boolean = partitioned.part2Length == 0

    final def nonEmpty: Boolean = !isEmpty

    final def partition(f: T => Boolean): (Part1[T], Part2[T]) = {
      val partitions = partitioned.part2Partition(f)
      partitions.part1 -> partitions.part2
    }

    final def foreach(f: T => Unit): Unit = partitioned.part2Foreach(f)
  }

  private class PartitionedArray[T](elems: Array[T], lastPart2: Int)(implicit tag: ClassTag[T]) extends Partitioned[T] {
    def part1Length: Int = lastPart2

    def part2Length: Int = elems.length - lastPart2

    def part1Foreach(f: T => Unit): Unit = {
      var i = 0
      while (i < part1Length) {
        f(elems(i))
        i += 1
      }
    }

    def part2Foreach(f: T => Unit): Unit = {
      var i = elems.length - 1
      while (i >= lastPart2) {
        f(elems(i))
        i -= 1
      }
    }

    def part1Partition(f: T => Boolean): Partitioned[T] = partition(part1Length, part1Foreach, f)

    def part2Partition(f: T => Boolean): Partitioned[T] = partition(part2Length, part2Foreach, f)

    private def remove(item: T): PartitionedArray[T] = {
      val removeIndex = elems.indexOf(item)
      if (removeIndex > 0) {
        val newArray = new Array[T](elems.length - 1)
        var i = 0
        while (i < removeIndex) {
          newArray(i) = elems(i)
          i += 1
        }
        i = removeIndex + 1
        while (i < elems.length) {
          newArray(i - 1) = elems(i)
          i += 1
        }
        new PartitionedArray(newArray, if (removeIndex < lastPart2) lastPart2 - 1 else lastPart2)
      } else {
        this
      }
    }

    def remove(items: Array[T]): Partitioned[T] = items.foldLeft(this)(_ remove _)
  }

  private class FreshAtomsTwoArrays[T](part1: Array[T], part2: Array[T])(implicit tag: ClassTag[T]) extends Partitioned[T] {
    def part1Length: Int = part1.length

    def part2Length: Int = part2.length

    def part1Foreach(f: T => Unit): Unit = part1.foreach(f)

    def part2Foreach(f: T => Unit): Unit = part2.foreach(f)

    def part1Partition(f: T => Boolean): Partitioned[T] = partition(part1.length, part1.foreach, f)

    def part2Partition(f: T => Boolean): Partitioned[T] = partition(part2.length, part2.foreach, f)

    private def removeFromPart(part: Array[T], index: Int): Array[T] = {
      val newArray = new Array[T](part.length - 1)
      var i = 0
      while (i < index) {
        newArray(i) = part(i)
        i += 1
      }
      i = index + 1
      while (i < part.length) {
        newArray(i - 1) = part(i)
        i += 1
      }
      newArray
    }

    private def remove(item: T): FreshAtomsTwoArrays[T] = {
      var i = part1.indexOf(item)
      if (i >= 0) {
        new FreshAtomsTwoArrays(removeFromPart(part1, i), part2)
      } else {
        i = part2.indexOf(item)
        if (i >= 0) {
          new FreshAtomsTwoArrays(part1, removeFromPart(part2, i))
        } else {
          this
        }
      }
    }

    def remove(items: Array[T]): Partitioned[T] = items.foldLeft(this)(_ remove _)
  }

  private def partition[T](partLength: Int, partForeach: (T => Unit) => Unit, f: T => Boolean)(implicit tag: ClassTag[T]): Partitioned[T] = {
    val newArray = new Array[T](partLength)
    var lastPart2 = newArray.length
    var lastPart1 = -1
    partForeach { item =>
      if (f(item)) {
        lastPart1 += 1
        newArray(lastPart1) = item
      } else {
        lastPart2 -= 1
        newArray(lastPart2) = item
      }
    }
    new PartitionedArray(newArray, lastPart2)
  }

  def from[T](it: IterableOnce[T])(f: T => Boolean)(implicit tag: ClassTag[T]): Partitioned[T] = {
    val (part1, part2) = it.iterator.toArray.partition(f)
    new FreshAtomsTwoArrays(part1, part2)
  }

}