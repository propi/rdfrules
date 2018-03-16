package eu.easyminer.rdf.index

import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.data.Quad
import eu.easyminer.rdf.index.TripleHashIndex.{HashMap, TripleObjectIndex, TriplePredicateIndex, TripleSubjectIndex}
import eu.easyminer.rdf.rule.{Atom, TripleItemPosition}

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex(val subjects: HashMap[Int, TripleSubjectIndex],
                      val predicates: HashMap[Int, TriplePredicateIndex],
                      val objects: HashMap[Int, TripleObjectIndex]) {
  lazy val size: Int = predicates.valuesIterator.map(_.size).sum
}

object TripleHashIndex {

  abstract class HashSet[T](set: java.util.Set[T]) {
    def iterator: Iterator[T] = set.iterator().asScala

    def apply(x: T): Boolean = set.contains(set)

    def size: Int = set.size()
  }

  class MutableHashSet[T](set: java.util.Set[T] = new java.util.HashSet[T]) extends HashSet(set) {
    def +=(x: T): Unit = set.add(x)
  }

  abstract class HashMap[K, V](map: java.util.Map[K, V]) {
    def apply(key: K): V = map.get(key)

    def keySet: HashSet[K] = new MutableHashSet(map.keySet())

    def valuesIterator: Iterator[V] = map.values().iterator().asScala
  }

  class MutableHashMap[K, V](map: java.util.Map[K, V] = new java.util.HashMap[K, V]) extends HashMap(map) {
    def getOrElseUpdate(key: K, default: => V): V = {
      var v = map.get(key)
      if (v == null) {
        v = default
        map.put(key, v)
      }
      v
    }
  }

  private val logger = Logger[TripleHashIndex]

  type TripleItemMap = HashMap[Int, HashSet[Int]]

  private def emptySet = new MutableHashSet[Int]

  private def emptyTripleItemMap = new MutableHashMap[Int, HashSet[Int]]

  class TripleItemMapWithGraphs(map: collection.Map[Int, TripleItemMap]) extends collection.Map[Int, collection.Set[Int]] {
    def get(key: Int): Option[collection.Set[Int]] = map.get(key).map(_.keySet)

    def iterator: Iterator[(Int, collection.Set[Int])] = ???

    def +[V1 >: collection.Set[Int]](kv: (Int, V1)): collection.Map[Int, V1] = ???

    def -(key: Int): collection.Map[Int, collection.Set[Int]] = ???
  }

  class TriplePredicateIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    private var graph: Option[Int] = None
    private val _graphs = emptySet
    private lazy val deepGraphs = collection.mutable.Map.empty[TripleItemPosition, collection.mutable.Set[Int]]

    private[TripleHashIndex] def addGraph(compressedQuad: CompressedQuad): Unit = if (graph.isEmpty) {
      graph = Some(compressedQuad.graph)
    } else {
      if (!graph.contains(compressedQuad.graph)) {
        if (_graphs.isEmpty) {
          val it1 = subjects.keysIterator.map(x => TripleItemPosition.Subject(Atom.Constant(x)))
          val it2 = objects.keysIterator.map(x => TripleItemPosition.Object(Atom.Constant(x)))
          for (tip <- it1 ++ it2) {
            deepGraphs.getOrElseUpdate(tip, emptySet) += graph.get
          }
        }
        _graphs += compressedQuad.graph
      }
      if (_graphs.nonEmpty) {
        for (tip <- Iterator(TripleItemPosition.Subject(Atom.Constant(compressedQuad.subject)), TripleItemPosition.Object(Atom.Constant(compressedQuad.`object`)))) {
          deepGraphs.getOrElseUpdate(tip, emptySet) += compressedQuad.graph
        }
      }
    }

    def graphs: Iterator[Int] = graph.iterator ++ _graphs.iterator

    def graphs(atomItem: TripleItemPosition): Iterator[Int] = if (_graphs.isEmpty) graph.iterator else deepGraphs(atomItem).iterator

    lazy val size: Int = subjects.valuesIterator.map(_.size).sum
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  def apply(quads: Traversable[CompressedQuad]): TripleHashIndex = {
    type M = MutableHashMap[Int, MutableHashSet[Int]]
    val tsi = collection.mutable.HashMap.empty[Int, TripleSubjectIndex]
    val tpi = collection.mutable.HashMap.empty[Int, TriplePredicateIndex]
    val toi = collection.mutable.HashMap.empty[Int, TripleObjectIndex]
    var i = 0
    for (quad <- quads) {
      val si = tsi.getOrElseUpdate(quad.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      val pi = tpi.getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMap, emptyTripleItemMap))
      val oi = toi.getOrElseUpdate(quad.`object`, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
      si.predicates.asInstanceOf[M].getOrElseUpdate(quad.predicate, emptySet) += quad.`object`
      si.objects.asInstanceOf[M].getOrElseUpdate(quad.`object`, emptySet) += quad.predicate
      pi.subjects.asInstanceOf[M].getOrElseUpdate(quad.subject, emptySet) += quad.`object`
      pi.objects.asInstanceOf[M].getOrElseUpdate(quad.`object`, emptySet) += quad.subject
      oi.predicates.asInstanceOf[M].getOrElseUpdate(quad.predicate, emptySet) += quad.subject
      oi.subjects.asInstanceOf[M].getOrElseUpdate(quad.subject, emptySet) += quad.predicate
      pi.addGraph(quad)
      i += 1
      if (i % 10000 == 0) logger.info(s"Dataset loading: $i quads")
    }
    logger.info(s"Dataset loaded: $i quads")
    new TripleHashIndex(tsi, tpi, toi)
  }

  def apply(quads: Traversable[Quad])(implicit mapper: TripleItemHashIndex): TripleHashIndex = apply(quads.view.map(_.toCompressedQuad))

}