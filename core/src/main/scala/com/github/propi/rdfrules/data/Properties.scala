package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.data.Properties.PropertyStats
import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
class Properties private(col: TripleTraversableView) {
  private class InnerPropertyStats extends PropertyStats {
    private val hmap = collection.mutable.Map.empty[TripleItemType, IncrementalInt]

    private[Properties] def +=(tripleItemType: TripleItemType): this.type = {
      hmap.getOrElseUpdate(tripleItemType, IncrementalInt()).++
      this
    }

    def sum(tripleItemType: TripleItemType): Int = hmap.get(tripleItemType).map(_.getValue).getOrElse(0)

    override def toString: String = hmap.iterator.map(x => s"${x._1}: ${x._2.getValue}").mkString(", ")
  }

  private lazy val map = {
    val map = collection.mutable.Map.empty[TripleItem.Uri, InnerPropertyStats]
    for (triple <- col) {
      map.getOrElseUpdate(triple.predicate, new InnerPropertyStats) += triple.`object`
    }
    map
  }

  def get(key: TripleItem.Uri): Option[PropertyStats] = map.get(key)

  def iterator: Iterator[(TripleItem.Uri, PropertyStats)] = map.iterator
}

object Properties {

  sealed trait PropertyStats {
    def sum(tripleItemType: TripleItemType): Int
  }

  def apply(col: TripleTraversableView): Properties = new Properties(col)

}