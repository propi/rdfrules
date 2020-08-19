package com.github.propi.rdfrules.index.ops

import java.io.{File, OutputStream}

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 19. 7. 2018.
  */
class IndexDecorator(index: Index) extends Index {

  implicit val debugger: Debugger = index.debugger

  def tripleMap[T](f: TripleHashIndex[Int] => T): T = index.tripleMap(f)

  def tripleItemMap[T](f: TripleItemHashIndex => T): T = index.tripleItemMap(f)

  def toDataset: Dataset = index.toDataset

  def cache(os: => OutputStream): Unit = index.cache(os)

  def cache(file: File): Index = index.cache(file)

  def cache(file: String): Index = index.cache(file)

  def withEvaluatedLazyVals: Index = index.withEvaluatedLazyVals

}
