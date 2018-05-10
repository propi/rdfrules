package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
object PredicateInfo {

  def apply(col: Traversable[Triple]): collection.Map[TripleItem.Uri, collection.Map[TripleItemType, Int]] = new collection.Map[TripleItem.Uri, collection.Map[TripleItemType, Int]] {
    private lazy val map = {
      val map = collection.mutable.Map.empty[TripleItem.Uri, collection.mutable.Map[TripleItemType, IncrementalInt]]
      for (triple <- col) {
        map.getOrElseUpdate(triple.predicate, collection.mutable.Map.empty).getOrElseUpdate(triple.`object`, IncrementalInt()).++
      }
      map.mapValues(_.mapValues(_.getValue))
    }

    def get(key: TripleItem.Uri): Option[collection.Map[TripleItemType, Int]] = map.get(key)

    def iterator: Iterator[(TripleItem.Uri, collection.Map[TripleItemType, Int])] = map.iterator

    def +[V1 >: collection.Map[TripleItemType, Int]](kv: (TripleItem.Uri, V1)): collection.Map[TripleItem.Uri, V1] = map + kv

    def -(key: TripleItem.Uri): collection.Map[TripleItem.Uri, collection.Map[TripleItemType, Int]] = map - key
  }

}