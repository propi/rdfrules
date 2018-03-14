package eu.easyminer.rdf.index

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.data.Quad
import eu.easyminer.rdf.index.TripleHashIndex.{TripleObjectIndex, TriplePredicateIndex, TripleSubjectIndex}

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

  type TripleItemMap = collection.Map[Int, collection.Map[Int, collection.Set[Int]]]

  class TriplePredicateIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    lazy val size: Int = subjects.valuesIterator.flatMap(_.valuesIterator.map(_.size)).sum
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.flatMap(_.valuesIterator.map(_.size)).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.flatMap(_.valuesIterator.map(_.size)).sum
  }

  private def emptyTripleItemMap = collection.mutable.HashMap.empty[Int, collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]]

  private def emptyGraphMap = collection.mutable.HashMap.empty[Int, collection.mutable.HashSet[Int]]

  private def emptySet = collection.mutable.HashSet.empty[Int]

  def apply(quads: Traversable[CompressedQuad]): TripleHashIndex = {
    type M1 = collection.mutable.HashMap[Int, collection.mutable.HashSet[Int]]
    type M2 = collection.mutable.HashMap[Int, M1]
    val tsi = collection.mutable.HashMap.empty[Int, TripleSubjectIndex]
    val tpi = collection.mutable.HashMap.empty[Int, TriplePredicateIndex]
    val toi = collection.mutable.HashMap.empty[Int, TripleObjectIndex]
    var i = 0
    for (quad <- quads) {
      val si = tsi.getOrElseUpdate(quad.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      val pi = tpi.getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMap, emptyTripleItemMap))
      val oi = toi.getOrElseUpdate(quad.`object`, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      si.predicates.asInstanceOf[M2].getOrElseUpdate(quad.predicate, emptyGraphMap).getOrElseUpdate(quad.`object`, emptySet) += quad.graph
      si.objects.asInstanceOf[M2].getOrElseUpdate(quad.`object`, emptyGraphMap).getOrElseUpdate(quad.predicate, emptySet) += quad.graph
      pi.subjects.asInstanceOf[M2].getOrElseUpdate(quad.subject, emptyGraphMap).getOrElseUpdate(quad.`object`, emptySet) += quad.graph
      pi.objects.asInstanceOf[M2].getOrElseUpdate(quad.`object`, emptyGraphMap).getOrElseUpdate(quad.subject, emptySet) += quad.graph
      oi.predicates.asInstanceOf[M2].getOrElseUpdate(quad.predicate, emptyGraphMap).getOrElseUpdate(quad.subject, emptySet) += quad.graph
      oi.subjects.asInstanceOf[M2].getOrElseUpdate(quad.subject, emptyGraphMap).getOrElseUpdate(quad.predicate, emptySet) += quad.graph
      i += 1
      if (i % 10000 == 0) logger.info(s"Dataset loading: $i quads")
    }
    logger.info(s"Dataset loaded: $i quads")
    new TripleHashIndex(tsi, tpi, toi)
  }

  def apply(quads: Traversable[Quad])(implicit mapper: TripleItemHashIndex): TripleHashIndex = apply(quads.view.map(_.toCompressedQuad))

}