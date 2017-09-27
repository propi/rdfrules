package eu.easyminer.rdf.data

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.data.TripleHashIndex.{TripleObjectIndex, TriplePredicateIndex, TripleSubjectIndex}

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex(val subjects: collection.Map[Int, TripleSubjectIndex],
                      val predicates: collection.Map[Int, TriplePredicateIndex],
                      val objects: collection.Map[Int, TripleObjectIndex]) {
  lazy val size: Int = predicates.valuesIterator.map(_.size).sum
}

object TripleHashIndex {

  private val logger = Logger[TripleHashIndex]

  type TripleItemMap = collection.Map[Int, collection.Set[Int]]

  class TriplePredicateIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    lazy val size: Int = subjects.valuesIterator.map(_.size).sum
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  private def emptyTripleItemMap = collection.mutable.HashMap.empty[Int, collection.Set[Int]]

  private def emptySet = collection.mutable.HashSet.empty[Int]

  def apply(it: Iterator[CompressedTriple]): TripleHashIndex = {
    val tsi = collection.mutable.HashMap.empty[Int, TripleSubjectIndex]
    val tpi = collection.mutable.HashMap.empty[Int, TriplePredicateIndex]
    val toi = collection.mutable.HashMap.empty[Int, TripleObjectIndex]
    var i = 0
    for (triple <- it) {
      val si = tsi.getOrElseUpdate(triple.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      val pi = tpi.getOrElseUpdate(triple.predicate, new TriplePredicateIndex(emptyTripleItemMap, emptyTripleItemMap))
      val oi = toi.getOrElseUpdate(triple.`object`, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      si.predicates.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.predicate, emptySet) += triple.`object`
      si.objects.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.`object`, emptySet) += triple.predicate
      pi.subjects.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.subject, emptySet) += triple.`object`
      pi.objects.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.`object`, emptySet) += triple.subject
      oi.predicates.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.predicate, emptySet) += triple.subject
      oi.subjects.asInstanceOf[collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]].getOrElseUpdate(triple.subject, emptySet) += triple.predicate
      i += 1
      if (i % 10000 == 0) logger.info(s"Dataset loading: $i triples")
    }
    logger.info(s"Dataset loaded: $i triples")
    new TripleHashIndex(tsi, tpi, toi)
  }

}
