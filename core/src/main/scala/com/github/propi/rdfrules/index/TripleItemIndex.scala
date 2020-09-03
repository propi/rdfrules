package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TripleItem

/**
  * Created by Vaclav Zeman on 3. 9. 2020.
  */
trait TripleItemIndex {

  def getNamespace(prefix: String): Option[String]

  def getIndex(x: TripleItem): Int = getIndexOpt(x).get

  def getIndexOpt(x: TripleItem): Option[Int]

  def getTripleItem(x: Int): TripleItem = getTripleItemOpt(x).get

  def getTripleItemOpt(x: Int): Option[TripleItem]

  def iterator: Iterator[(Int, TripleItem)]

  def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemIndex

}