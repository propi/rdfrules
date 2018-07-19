package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Quad
import com.github.propi.rdfrules.index.TripleHashIndex._
import com.github.propi.rdfrules.rule.{Atom, TripleItemPosition}
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.language.implicitConversions

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
  private var severalGraphs: Boolean = false

  lazy val size: Int = predicates.valuesIterator.map(_.size).sum

  def evaluateAllLazyVals(): Unit = {
    size
    subjects.valuesIterator.foreach(_.size)
    objects.valuesIterator.foreach(_.size)
    if (graph.isEmpty) {
      predicates.valuesIterator.foreach(_.graphs)
    }
  }

  def getGraphs(predicate: Int): HashSet[Int] = if (graph.nonEmpty) {
    val set = new MutableHashSet[Int]
    set += graph.get
    set
  } else {
    predicates(predicate).graphs
  }

  def getGraphs(predicate: Int, tripleItemPosition: TripleItemPosition): HashSet[Int] = if (graph.nonEmpty) {
    val set = new MutableHashSet[Int]
    set += graph.get
    set
  } else {
    val pi = predicates(predicate)
    tripleItemPosition match {
      case TripleItemPosition.Subject(Atom.Constant(x)) => pi.subjects(x).graphs
      case TripleItemPosition.Object(Atom.Constant(x)) => pi.objects(x).graphs
      case _ => pi.graphs
    }
  }

  def getGraphs(subject: Int, predicate: Int, `object`: Int): HashSet[Int] = if (graph.nonEmpty) {
    val set = new MutableHashSet[Int]
    set += graph.get
    set
  } else {
    predicates(predicate).subjects(subject).apply(`object`)
  }

  private def addGraph(quad: CompressedQuad): Unit = {
    //get predicate index by a specific predicate
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex]]
      .getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMapWithGraphs, emptyTripleItemMapWithGraphs))
    //get predicate-subject index by a specific subject
    val psi = pi.subjects.asInstanceOf[M[AnyWithGraphs[TripleItemMap]]].getOrElseUpdate(quad.subject, emptyMapWithGraphs).asInstanceOf[MutableAnyWithGraphs[TripleItemMap]]
    //add graph to this predicate-subject index - it is suitable for atom p(A, b) to enumerate all graphs
    psi.addGraph(quad.graph)
    //get predicate-subject-object index by a specific object and add the graph
    // - it is suitable for enumerate all quads with graphs
    // - then construct Dataset from Index
    psi.value.asInstanceOf[M1].getOrElseUpdate(quad.`object`, emptySet) += quad.graph
    //get predicate-object index by a specific object and add the graph - it is suitable for atom p(a, B) to enumerate all graphs
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet[Int]]]].getOrElseUpdate(quad.`object`, emptySetWithGraphs).asInstanceOf[MutableAnyWithGraphs[HashSet[Int]]].addGraph(quad.graph)
  }

  private def addQuad(quad: CompressedQuad): Unit = {
    if (graph.isEmpty) {
      if (severalGraphs) {
        addGraph(quad)
      } else {
        graph = Some(quad.graph)
      }
    } else if (!graph.contains(quad.graph)) {
      for {
        (p, m1) <- predicates.iterator
        (o, m2) <- m1.objects.iterator
        s <- m2.iterator
        g <- graph
      } {
        addGraph(CompressedQuad(s, p, o, g))
      }
      addGraph(quad)
      severalGraphs = true
      graph = None
    }
    val si = subjects.asInstanceOf[M[TripleSubjectIndex]].getOrElseUpdate(quad.subject, new TripleSubjectIndex(emptyTripleItemMap, emptyTripleItemMap))
    val oi = objects.asInstanceOf[M[TripleObjectIndex]].getOrElseUpdate(quad.`object`, new TripleObjectIndex(emptyTripleItemMap, emptyTripleItemMap))
    si.predicates.asInstanceOf[M1].getOrElseUpdate(quad.predicate, emptySet) += quad.`object`
    si.objects.asInstanceOf[M1].getOrElseUpdate(quad.`object`, emptySet) += quad.predicate
    oi.predicates.asInstanceOf[M1].getOrElseUpdate(quad.predicate, emptySet) += quad.subject
    oi.subjects.asInstanceOf[M1].getOrElseUpdate(quad.subject, emptySet) += quad.predicate
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex]].getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMapWithGraphs[TripleItemMap], emptyTripleItemMapWithGraphs[HashSet[Int]]))
    if (!severalGraphs) {
      pi.subjects.asInstanceOf[M[AnyWithGraphs[TripleItemMap]]]
        .getOrElseUpdate(quad.subject, emptyMapWithGraphs).value.asInstanceOf[M1]
        .getOrElseUpdate(quad.`object`, emptySet)
    }
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet[Int]]]].getOrElseUpdate(quad.`object`, emptySetWithGraphs).value.asInstanceOf[MutableHashSet[Int]] += quad.subject
  }

}

object TripleHashIndex {

  private val logger = Logger[TripleHashIndex]

  type TripleItemMap = HashMap[Int, HashSet[Int]]
  type TripleItemMapWithGraphsAndSet = HashMap[Int, AnyWithGraphs[HashSet[Int]]]
  type TripleItemMapWithGraphsAndMap = HashMap[Int, AnyWithGraphs[TripleItemMap]]

  abstract class HashSet[T](set: java.util.Set[T]) {
    def iterator: Iterator[T] = set.iterator().asScala

    def apply(x: T): Boolean = set.contains(x)

    def size: Int = set.size()
  }

  class MutableHashSet[T](set: java.util.Set[T] = new java.util.HashSet[T]) extends HashSet(set) {
    def +=(x: T): Unit = set.add(x)
  }

  class AnyWithGraphs[T](val value: T, val graphs: HashSet[Int])

  class MutableAnyWithGraphs[T](value: T, graphs: MutableHashSet[Int]) extends AnyWithGraphs[T](value, graphs) {
    def addGraph(g: Int): Unit = graphs += g
  }

  implicit def anyWithGraphsTo[T](hashSetWithGraphs: AnyWithGraphs[T]): T = hashSetWithGraphs.value

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

  private def emptySetWithGraphs = new MutableAnyWithGraphs[HashSet[Int]](emptySet, emptySet)

  private def emptyMapWithGraphs = new MutableAnyWithGraphs[TripleItemMap](emptyTripleItemMap, emptySet)

  private def emptyTripleItemMap = new MutableHashMap[Int, HashSet[Int]]

  private def emptyTripleItemMapWithGraphs[T] = new MutableHashMap[Int, AnyWithGraphs[T]]

  class TriplePredicateIndex(val subjects: TripleItemMapWithGraphsAndMap, val objects: TripleItemMapWithGraphsAndSet) {
    lazy val size: Int = subjects.valuesIterator.map(_.size).sum
    //add all graphs to this predicate index - it is suitable for atom p(a, b) to enumerate all graphs
    //it is contructed from all predicate-subject graphs
    lazy val graphs: HashSet[Int] = {
      val set = emptySet
      subjects.valuesIterator.flatMap(_.graphs.iterator).foreach(set += _)
      set
    }
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