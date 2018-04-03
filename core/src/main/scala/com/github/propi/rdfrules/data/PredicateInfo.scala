package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
case class PredicateInfo private(uri: TripleItem.Uri, ranges: collection.Map[TripleItemType, Int])

object PredicateInfo {

  def apply(col: Traversable[Triple]): Traversable[PredicateInfo] = new Traversable[PredicateInfo] {
    def foreach[U](f: PredicateInfo => U): Unit = {
      val map = collection.mutable.Map.empty[TripleItem.Uri, collection.mutable.Map[TripleItemType, IncrementalInt]]
      for (triple <- col) {
        map.getOrElseUpdate(triple.predicate, collection.mutable.Map.empty).getOrElseUpdate(triple.`object`, IncrementalInt()).++
      }
      for ((uri, map) <- map) {
        f(PredicateInfo(uri, map.mapValues(_.getValue)))
      }
    }
  }

}