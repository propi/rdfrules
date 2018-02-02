package eu.easyminer.rdf.data

import eu.easyminer.rdf.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
case class PredicateInfo private(uri: TripleItem.Uri, ranges: collection.Map[TripleItemType, Int])

object PredicateInfo {

  def apply(col: TraversableOnce[Triple]): Vector[PredicateInfo] = {
    val map = collection.mutable.Map.empty[TripleItem.Uri, collection.mutable.Map[TripleItemType, IncrementalInt]]
    for (triple <- col) {
      map.getOrElseUpdate(triple.predicate, collection.mutable.Map.empty).getOrElseUpdate(triple.`object`, IncrementalInt()).++
    }
    map.iterator.map { case (uri, map) =>
      new PredicateInfo(uri, map.mapValues(_.getValue))
    }.toVector
  }

}
