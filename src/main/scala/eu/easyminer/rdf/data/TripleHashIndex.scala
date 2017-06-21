package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
object TripleHashIndex {

  type TripleMap = collection.mutable.Map[String, TripleIndex]
  type TripleItemMap = collection.mutable.Map[String, collection.mutable.Set[String]]

  class TripleIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    val size = subjects.valuesIterator.map(_.size).sum
  }

  def apply(it: Iterator[Triple]): TripleMap = {
    collection.mutable.HashMap(
      it.toIterable
        .groupBy(_.predicate)
        .mapValues(x =>
          new TripleIndex(
            collection.mutable.HashMap(x.groupBy(_.subject).mapValues(x => collection.mutable.HashSet(x.map(_.`object`.toStringValue).toList: _*)).toList: _*),
            collection.mutable.HashMap(x.groupBy(_.`object`.toStringValue).mapValues(x => collection.mutable.HashSet(x.map(_.subject).toList: _*)).toList: _*)
          )
        )
        .toSeq: _*
    )
  }

}
