package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.TripleHashIndex.{TripleObjectIndex, TriplePredicateIndex, TripleSubjectIndex}

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex(val subjects: collection.mutable.Map[String, TripleSubjectIndex],
                      val predicates: collection.mutable.Map[String, TriplePredicateIndex],
                      val objects: collection.mutable.Map[String, TripleObjectIndex]) {
  lazy val size = predicates.valuesIterator.map(_.size).sum
}

object TripleHashIndex {

  type TripleItemMap = collection.mutable.Map[String, collection.mutable.Set[String]]

  class TriplePredicateIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    lazy val size = subjects.valuesIterator.map(_.size).sum
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size = predicates.valuesIterator.map(_.size).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size = predicates.valuesIterator.map(_.size).sum
  }

  private def emptyTripleItemMap = collection.mutable.HashMap.empty[String, collection.mutable.Set[String]]

  private def emptySet = collection.mutable.HashSet.empty[String]

  def apply(it: Iterator[Triple]) = {
    val tsi = collection.mutable.HashMap.empty[String, TripleSubjectIndex]
    val tpi = collection.mutable.HashMap.empty[String, TriplePredicateIndex]
    val toi = collection.mutable.HashMap.empty[String, TripleObjectIndex]
    for (triple <- it) {
      val si = tsi.getOrElseUpdate(triple.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      val pi = tpi.getOrElseUpdate(triple.predicate, new TriplePredicateIndex(emptyTripleItemMap, emptyTripleItemMap))
      val oi = toi.getOrElseUpdate(triple.`object`.toStringValue, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      si.predicates.getOrElseUpdate(triple.predicate, emptySet) += triple.`object`.toStringValue
      si.objects.getOrElseUpdate(triple.`object`.toStringValue, emptySet) += triple.predicate
      pi.subjects.getOrElseUpdate(triple.subject, emptySet) += triple.`object`.toStringValue
      pi.objects.getOrElseUpdate(triple.`object`.toStringValue, emptySet) += triple.subject
      oi.predicates.getOrElseUpdate(triple.predicate, emptySet) += triple.subject
      oi.subjects.getOrElseUpdate(triple.subject, emptySet) += triple.predicate
    }
    new TripleHashIndex(tsi, tpi, toi)
  }

}
