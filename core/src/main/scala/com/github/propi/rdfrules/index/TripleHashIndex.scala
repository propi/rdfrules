package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.Quad
import com.github.propi.rdfrules.index.TripleHashIndex._
import com.github.propi.rdfrules.rule.{Atom, TripleItemPosition}
import com.github.propi.rdfrules.utils.Debugger
import it.unimi.dsi.fastutil.ints.{Int2ReferenceOpenHashMap, IntOpenHashSet}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex private {

  type M[T] = MutableHashMap[T]
  type M1 = M[MutableHashSet]

  val subjects: HashMap[TripleSubjectIndex] = new MutableHashMap[TripleSubjectIndex]
  val predicates: HashMap[TriplePredicateIndex] = new MutableHashMap[TriplePredicateIndex]
  val objects: HashMap[TripleObjectIndex] = new MutableHashMap[TripleObjectIndex]

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

  def getGraphs(predicate: Int): HashSet = if (graph.nonEmpty) {
    val set = new MutableHashSet
    set += graph.get
    set
  } else {
    predicates(predicate).graphs
  }

  def getGraphs(predicate: Int, tripleItemPosition: TripleItemPosition): HashSet = if (graph.nonEmpty) {
    val set = new MutableHashSet
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

  def getGraphs(subject: Int, predicate: Int, `object`: Int): HashSet = if (graph.nonEmpty) {
    val set = new MutableHashSet
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
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet]]].getOrElseUpdate(quad.`object`, emptySetWithGraphs).asInstanceOf[MutableAnyWithGraphs[HashSet]].addGraph(quad.graph)
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
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex]].getOrElseUpdate(quad.predicate, new TriplePredicateIndex(emptyTripleItemMapWithGraphs[TripleItemMap], emptyTripleItemMapWithGraphs[HashSet]))
    if (!severalGraphs) {
      pi.subjects.asInstanceOf[M[AnyWithGraphs[TripleItemMap]]]
        .getOrElseUpdate(quad.subject, emptyMapWithGraphs).value.asInstanceOf[M1]
        .getOrElseUpdate(quad.`object`, emptySet)
    }
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet]]].getOrElseUpdate(quad.`object`, emptySetWithGraphs).value.asInstanceOf[MutableHashSet] += quad.subject
  }

  def trim(): Unit = {
    for (x <- subjects.valuesIterator) {
      for (x <- x.predicates.valuesIterator) x.trim()
      for (x <- x.objects.valuesIterator) x.trim()
      x.predicates.trim()
      x.objects.trim()
    }
    for (x <- objects.valuesIterator) {
      for (x <- x.predicates.valuesIterator) x.trim()
      for (x <- x.subjects.valuesIterator) x.trim()
      x.predicates.trim()
      x.subjects.trim()
    }
    for (x <- predicates.valuesIterator) {
      for (x <- x.subjects.valuesIterator) {
        x.value.trim()
        x.graphs.trim()
        for (x <- x.value.valuesIterator) x.trim()
      }
      for (x <- x.objects.valuesIterator) {
        x.value.trim()
        x.graphs.trim()
      }
      x.subjects.trim()
      x.objects.trim()
    }
    predicates.trim()
    subjects.trim()
    objects.trim()
  }

}

object TripleHashIndex {

  type TripleItemMap = HashMap[HashSet]
  type TripleItemMapWithGraphsAndSet = HashMap[AnyWithGraphs[HashSet]]
  type TripleItemMapWithGraphsAndMap = HashMap[AnyWithGraphs[TripleItemMap]]

  abstract class HashSet(set: java.util.Set[Integer]) {
    def iterator: Iterator[Int] = set.iterator().asScala.map(_.intValue())

    def apply(x: Int): Boolean = set.contains(x)

    def size: Int = set.size()

    def trim(): Unit = set match {
      case x: IntOpenHashSet => x.trim()
      case _ =>
    }

    def isEmpty: Boolean = set.isEmpty
  }

  class MutableHashSet(set: java.util.Set[Integer] = new IntOpenHashSet()) extends HashSet(set) {
    def +=(x: Int): Unit = set.add(x)
  }

  class AnyWithGraphs[T](val value: T, val graphs: HashSet)

  class MutableAnyWithGraphs[T](value: T, graphs: MutableHashSet) extends AnyWithGraphs[T](value, graphs) {
    def addGraph(g: Int): Unit = graphs += g
  }

  implicit def anyWithGraphsTo[T](hashSetWithGraphs: AnyWithGraphs[T]): T = hashSetWithGraphs.value

  abstract class HashMap[V](map: java.util.Map[Integer, V]) {
    def apply(key: Int): V = {
      val x = map.get(key)
      if (x == null) throw new NoSuchElementException else x
    }

    def keySet: HashSet = new MutableHashSet(map.keySet())

    def get(key: Int): Option[V] = Option(map.get(key))

    def keysIterator: Iterator[Int] = map.keySet().iterator().asScala.map(_.intValue())

    def valuesIterator: Iterator[V] = map.values().iterator().asScala

    def iterator: Iterator[(Int, V)] = map.entrySet().iterator().asScala.map(x => x.getKey.intValue() -> x.getValue)

    def size: Int = map.size()

    def isEmpty: Boolean = map.isEmpty

    def contains(key: Int): Boolean = map.containsKey(key)

    def trim(): Unit = map match {
      case x: Int2ReferenceOpenHashMap[_] => x.trim()
      case _ =>
    }
  }

  class MutableHashMap[V](map: java.util.Map[Integer, V] = new Int2ReferenceOpenHashMap[V]()) extends HashMap(map) {
    def getOrElseUpdate(key: Int, default: => V): V = {
      var v = map.get(key)
      if (v == null) {
        v = default
        map.put(key, v)
      }
      v
    }
  }

  private def emptySet = new MutableHashSet

  private def emptySetWithGraphs = new MutableAnyWithGraphs[HashSet](emptySet, emptySet)

  private def emptyMapWithGraphs = new MutableAnyWithGraphs[TripleItemMap](emptyTripleItemMap, emptySet)

  private def emptyTripleItemMap = new MutableHashMap[HashSet]

  private def emptyTripleItemMapWithGraphs[T] = new MutableHashMap[AnyWithGraphs[T]]

  class TriplePredicateIndex(val subjects: TripleItemMapWithGraphsAndMap, val objects: TripleItemMapWithGraphsAndSet) {
    lazy val size: Int = subjects.valuesIterator.map(_.size).sum
    //add all graphs to this predicate index - it is suitable for atom p(a, b) to enumerate all graphs
    //it is contructed from all predicate-subject graphs
    lazy val graphs: HashSet = {
      val set = emptySet
      subjects.valuesIterator.flatMap(_.graphs.iterator).foreach(set += _)
      set.trim()
      set
    }
  }

  class TripleSubjectIndex(val objects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  class TripleObjectIndex(val subjects: TripleItemMap, val predicates: TripleItemMap) {
    lazy val size: Int = predicates.valuesIterator.map(_.size).sum
  }

  def apply(quads: Traversable[CompressedQuad])(implicit debugger: Debugger): TripleHashIndex = {
    val index = new TripleHashIndex
    debugger.debug("Dataset indexing") { ad =>
      for (quad <- quads) {
        index.addQuad(quad)
        ad.done()
      }
      index.trim()
    }
    index
  }

  def apply(quads: Traversable[Quad])(implicit mapper: TripleItemHashIndex, debugger: Debugger): TripleHashIndex = apply(quads.view.filter(!_.triple.predicate.hasSameUriAs("http://www.w3.org/2002/07/owl#sameAs")).map(_.toCompressedQuad))

}