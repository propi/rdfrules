package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.index.Index.PartType
import com.github.propi.rdfrules.index.{Index, IndexPart, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

class SingleIndex private[index](val main: IndexPart) extends TrainTestIndex with Cacheable {
  def testIsTrain: Boolean = true

  def train: IndexPart = main

  def test: IndexPart = main

  def merged: IndexPart = main

  def part(partType: PartType): Option[IndexPart] = if (partType == PartType.Train) Some(main) else None

  def parts: Iterator[(PartType, IndexPart)] = Iterator(PartType.Train -> main)

  def tripleItemMap: TripleItemIndex = main.tripleItemMap

  def withDebugger(implicit debugger: Debugger): Index = new SingleIndex(main.withDebugger)
}
