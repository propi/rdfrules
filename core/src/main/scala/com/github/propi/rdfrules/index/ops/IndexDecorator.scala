package com.github.propi.rdfrules.index.ops

import java.io.{File, OutputStream}

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.{Index, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 19. 7. 2018.
  */
class IndexDecorator(index: Index) extends Index {

  implicit val debugger: Debugger = index.debugger

  def tripleMap[T](f: TripleIndex[Int] => T): T = index.tripleMap(f)

  def tripleItemMap[T](f: TripleItemIndex => T): T = index.tripleItemMap(f)

  def toDataset: Dataset = index.toDataset

  def cache(os: => OutputStream): Unit = index.cache(os)

  def cache(file: File): Index = index.cache(file)

  def cache(file: String): Index = index.cache(file)

  def withDebugger(implicit debugger: Debugger): Index = index.withDebugger

  override def withEvaluatedLazyVals: Index = index.withEvaluatedLazyVals

}