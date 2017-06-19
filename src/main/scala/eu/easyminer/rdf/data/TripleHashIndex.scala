package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
object TripleHashIndex {

  type TripleMap = collection.mutable.Map[String, TripleIndex]

  class TripleIndex(val subjects: collection.Map[String, collection.Set[String]], val objects: collection.Map[String, collection.Set[String]])

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
