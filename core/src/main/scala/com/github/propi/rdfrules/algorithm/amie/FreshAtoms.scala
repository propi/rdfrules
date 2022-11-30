package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.algorithm.amie.FreshAtoms.{Part1, Part2}
import com.github.propi.rdfrules.rule.FreshAtom

trait FreshAtoms {
  def part1Length: Int

  def part2Length: Int

  def part1Foreach(f: FreshAtom => Unit): Unit

  def part2Foreach(f: FreshAtom => Unit): Unit

  def part1Partition(f: FreshAtom => Boolean): FreshAtoms

  def part2Partition(f: FreshAtom => Boolean): FreshAtoms

  def remove(freshAtoms: Array[FreshAtom]): FreshAtoms

  final def part1: Part1 = new Part1(this)

  final def part2: Part2 = new Part2(this)
}

object FreshAtoms {

  class Part1(val freshAtoms: FreshAtoms) extends AnyVal {
    final def isEmpty: Boolean = freshAtoms.part1Length == 0

    final def nonEmpty: Boolean = !isEmpty

    final def partition(f: FreshAtom => Boolean): (Part1, Part2) = {
      val partitions = freshAtoms.part1Partition(f)
      partitions.part1 -> partitions.part2
    }

    final def foreach(f: FreshAtom => Unit): Unit = freshAtoms.part1Foreach(f)
  }

  class Part2(val freshAtoms: FreshAtoms) extends AnyVal {
    final def isEmpty: Boolean = freshAtoms.part2Length == 0

    final def nonEmpty: Boolean = !isEmpty

    final def partition(f: FreshAtom => Boolean): (Part1, Part2) = {
      val partitions = freshAtoms.part2Partition(f)
      partitions.part1 -> partitions.part2
    }

    final def foreach(f: FreshAtom => Unit): Unit = freshAtoms.part2Foreach(f)
  }

  private class FreshAtomsArray(elems: Array[FreshAtom], lastPart2: Int) extends FreshAtoms {
    def part1Length: Int = lastPart2

    def part2Length: Int = elems.length - lastPart2

    def part1Foreach(f: FreshAtom => Unit): Unit = {
      var i = 0
      while (i < part1Length) {
        f(elems(i))
        i += 1
      }
    }

    def part2Foreach(f: FreshAtom => Unit): Unit = {
      var i = elems.length - 1
      while (i >= lastPart2) {
        f(elems(i))
        i -= 1
      }
    }

    def part1Partition(f: FreshAtom => Boolean): FreshAtoms = partition(part1Length, part1Foreach, f)

    def part2Partition(f: FreshAtom => Boolean): FreshAtoms = partition(part2Length, part2Foreach, f)

    private def remove(freshAtom: FreshAtom): FreshAtomsArray = {
      val removeIndex = elems.indexOf(freshAtom)
      if (removeIndex > 0) {
        val newArray = new Array[FreshAtom](elems.length - 1)
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
        new FreshAtomsArray(newArray, if (removeIndex < lastPart2) lastPart2 - 1 else lastPart2)
      } else {
        this
      }
    }

    def remove(freshAtoms: Array[FreshAtom]): FreshAtoms = freshAtoms.foldLeft(this)(_ remove _)
  }

  private class FreshAtomsTwoArrays(part1: Array[FreshAtom], part2: Array[FreshAtom]) extends FreshAtoms {
    def part1Length: Int = part1.length

    def part2Length: Int = part2.length

    def part1Foreach(f: FreshAtom => Unit): Unit = part1.foreach(f)

    def part2Foreach(f: FreshAtom => Unit): Unit = part2.foreach(f)

    def part1Partition(f: FreshAtom => Boolean): FreshAtoms = partition(part1.length, part1.foreach, f)

    def part2Partition(f: FreshAtom => Boolean): FreshAtoms = partition(part2.length, part2.foreach, f)

    private def removeFromPart(part: Array[FreshAtom], index: Int): Array[FreshAtom] = {
      val newArray = new Array[FreshAtom](part.length - 1)
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

    private def remove(freshAtom: FreshAtom): FreshAtomsTwoArrays = {
      var i = part1.indexOf(freshAtom)
      if (i >= 0) {
        new FreshAtomsTwoArrays(removeFromPart(part1, i), part2)
      } else {
        i = part2.indexOf(freshAtom)
        if (i >= 0) {
          new FreshAtomsTwoArrays(part1, removeFromPart(part2, i))
        } else {
          this
        }
      }
    }

    def remove(freshAtoms: Array[FreshAtom]): FreshAtoms = freshAtoms.foldLeft(this)(_ remove _)
  }

  private def partition(partLength: Int, partForeach: (FreshAtom => Unit) => Unit, f: FreshAtom => Boolean): FreshAtoms = {
    val newArray = new Array[FreshAtom](partLength)
    var lastPart2 = newArray.length
    var lastPart1 = -1
    partForeach { freshAtom =>
      if (f(freshAtom)) {
        lastPart1 += 1
        newArray(lastPart1) = freshAtom
      } else {
        lastPart2 -= 1
        newArray(lastPart2) = freshAtom
      }
    }
    new FreshAtomsArray(newArray, lastPart2)
  }

  def from(it: IterableOnce[FreshAtom])(f: FreshAtom => Boolean): FreshAtoms = {
    val (part1, part2) = it.iterator.toArray.partition(f)
    new FreshAtomsTwoArrays(part1, part2)
  }

}