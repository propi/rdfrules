package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Quad
import com.github.propi.rdfrules.index.TripleHashIndex._
import com.github.propi.rdfrules.rule.{Atom, TripleItemPosition}
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex private {

  type M[T] = MutableHashMap[Int, T]
  type M1 = M[MutableHashSet[Int]]

  val subjects: HashMap[Int, TripleSubjectIndex] = new MutableHashMap[Int, TripleSubjectIndex]
  val predicates: HashMap[Int, TriplePredicateIndex] = new MutableHashMap[Int, TriplePredicateIndex]
  val objects: HashMap[Int, TripleObjectIndex] = new MutableHashMap[Int, TripleObjectIndex]

  private var graph: Option[Int] = None
  private val graphs: HashMap[Int, HashMap[Int, TripleGraphIndex]] = new MutableHashMap[Int, HashMap[Int, TripleGraphIndex]]

  lazy val size: Int = predicates.valuesIterator.map(_.size).sum

  private def tripleItemPositionExists(tripleItemPosition: TripleItemPosition)(tgi: TripleGraphIndex): Boolean = tripleItemPosition match {
    case TripleItemPosition.Subject(x) => x match {
      case Atom.Constant(x) => tgi.subjects.contains(x)
      case _ => true
    }
    case TripleItemPosition.Object(x) => x match {
      case Atom.Constant(x) => tgi.objects(x)
      case _ => true
    }
  }

  def isInGraph(graph: Int, predicate: Int): Boolean = (graphs.isEmpty && this.graph.contains(graph)) || graphs.get(graph).exists(_.contains(predicate))

  def getGraphs(predicate: Int): Iterator[Int] = if (graphs.isEmpty) Iterator(graph.get) else graphs.iterator.filter(_._2.contains(predicate)).map(_._1)

  def isInGraph(graph: Int, predicate: Int, tripleItemPosition: TripleItemPosition): Boolean = {
    val f = tripleItemPositionExists(tripleItemPosition) _
    (graphs.isEmpty && this.graph.contains(graph)) || graphs.get(graph).exists(_.get(predicate).exists(f))
  }

  def getGraphs(predicate: Int, tripleItemPosition: TripleItemPosition): Iterator[Int] = {
    val f = tripleItemPositionExists(tripleItemPosition) _
    if (graphs.isEmpty) Iterator(graph.get) else graphs.iterator.filter(_._2.get(predicate).exists(f)).map(_._1)
  }

  def isInGraph(graph: Int, subject: Int, predicate: Int, `object`: Int): Boolean = {
    (graphs.isEmpty && this.graph.contains(graph)) || graphs.get(graph).exists(_.get(predicate).exists(_.subjects.get(subject).exists(_.apply(`object`))))
  }

  def getGraphs(subject: Int, predicate: Int, `object`: Int): Iterator[Int] = {
    if (graphs.isEmpty) Iterator(graph.get) else graphs.iterator.filter(_._2.get(predicate).exists(_.subjects.get(subject).exists(_.apply(`object`)))).map(_._1)
  }

  private def addGraph(quad: CompressedQuad): Unit = {
    val gi = graphs.asInstanceOf[M[MutableHashMap[Int, TripleGraphIndex]]]
      .getOrElseUpdate(quad.graph, new MutableHashMap[Int, TripleGraphIndex])
      .getOrElseUpdate(quad.predicate, new TripleGraphIndex(emptyTripleItemMap, emptySet))
    gi.objects.asInstanceOf[MutableHashSet[Int]] += quad.`object`
    gi.subjects.asInstanceOf[M1].getOrElseUpdate(quad.subject, emptySet) += quad.`object`
  }

  private def addQuad(quad: CompressedQuad): Unit = {
    if (graph.isEmpty) {
      graph = Some(quad.graph)
    } else if (!graphs.isEmpty) {
      addGraph(quad)
    } else if (!graph.contains(quad.graph)) {
      for {
        (p, m1) <- predicates.iterator
        (s, m2) <- m1.subjects.iterator
        o <- m2.iterator
        g <- graph
      } {
        addGraph(CompressedQuad(s, p, o, g))
      }
      addGraph(quad)
    }
    val si = subjects.asInstanceOf[M[TripleSubjectIndex]].getOrElseUpdate(quad.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex]].getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMap, emptyTripleItemMap))
    val oi = objects.asInstanceOf[M[TripleObjectIndex]].getOrElseUpdate(quad.`object`, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
    si.predicates.asInstanceOf[M1].getOrElseUpdate(quad.predicate, emptySet) += quad.`object`
    si.objects.asInstanceOf[M1].getOrElseUpdate(quad.`object`, emptySet) += quad.predicate
    pi.subjects.asInstanceOf[M1].getOrElseUpdate(quad.subject, emptySet) += quad.`object`
    pi.objects.asInstanceOf[M1].getOrElseUpdate(quad.`object`, emptySet) += quad.subject
    oi.predicates.asInstanceOf[M1].getOrElseUpdate(quad.predicate, emptySet) += quad.subject
    oi.subjects.asInstanceOf[M1].getOrElseUpdate(quad.subject, emptySet) += quad.predicate
  }

}

object TripleHashIndex {

  private val logger = Logger[TripleHashIndex]

  type TripleItemMap = HashMap[Int, HashSet[Int]]

  abstract class HashSet[T](set: java.util.Set[T]) {
    def iterator: Iterator[T] = set.iterator().asScala

    def apply(x: T): Boolean = set.contains(x)

    def size: Int = set.size()
  }

  class MutableHashSet[T](set: java.util.Set[T] = new java.util.HashSet[T]) extends HashSet(set) {
    def +=(x: T): Unit = set.add(x)
  }

  abstract class HashMap[K, V](map: java.util.Map[K, V]) {
    def apply(key: K): V = {
      val x = map.get(key)
      if (x == null) throw new NoSuchElementException else x
    }

    def keySet: HashSet[K] = new MutableHashSet(map.keySet())

    def get(key: K): Option[V] = Option(map.get(key))

    def keysIterator: Iterator[K] = map.keySet().iterator().asScala

    def valuesIterator: Iterator[V] = map.values().iterator().asScala

    def iterator: Iterator[(K, V)] = map.entrySet().iterator().asScala.map(x => x.getKey -> x.getValue)

    def size: Int = map.size()

    def isEmpty: Boolean = map.isEmpty

    def contains(key: K): Boolean = map.containsKey(key)
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

  private def emptySet = new MutableHashSet[Int]

  private def emptyTripleItemMap = new MutableHashMap[Int, HashSet[Int]]

  class TripleGraphIndex(val subjects: TripleItemMap, val objects: HashSet[Int])

  class TriplePredicateIndex(val subjects: TripleItemMap, val objects: TripleItemMap) {
    lazy val size: Int = subjects.valuesIterator.map(_.size).sum
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  def apply(quads: Traversable[CompressedQuad]): TripleHashIndex = {
    val index = new TripleHashIndex
    var i = 0
    for (quad <- quads) {
      index.addQuad(quad)
      i += 1
      if (i % 10000 == 0) logger.info(s"Dataset loading: $i quads")
    }
    logger.info(s"Dataset loaded: $i quads")
    index
  }

  def apply(quads: Traversable[Quad])(implicit mapper: TripleItemHashIndex): TripleHashIndex = apply(quads.view.map(_.toCompressedQuad))

}