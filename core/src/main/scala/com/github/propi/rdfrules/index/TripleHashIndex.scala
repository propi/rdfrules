package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.index.TripleHashIndex._
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.Debugger

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex[T] private(implicit collectionsBuilder: CollectionsBuilder[T]) {

  type M[V] = MutableHashMap[T, V]
  type M1 = M[MutableHashSet[T]]

  val subjects: HashMap[T, TripleSubjectIndex[T]] = collectionsBuilder.emptyHashMap
  val predicates: HashMap[T, TriplePredicateIndex[T]] = collectionsBuilder.emptyHashMap
  val objects: HashMap[T, TripleObjectIndex[T]] = collectionsBuilder.emptyHashMap

  private var graph: Option[T] = None
  private var severalGraphs: Boolean = false

  @volatile private var _size: Int = -1

  def size: Int = {
    if (_size == -1) {
      _size = predicates.valuesIterator.map(_.size).sum
    }
    _size
  }

  def reset(): Unit = {
    _size = -1
    subjects.valuesIterator.foreach(_.reset())
    predicates.valuesIterator.foreach(_.reset())
    objects.valuesIterator.foreach(_.reset())
  }

  def evaluateAllLazyVals(): Unit = {
    size
    subjects.valuesIterator.foreach(_.size)
    objects.valuesIterator.foreach(_.size)
    if (graph.isEmpty) {
      predicates.valuesIterator.foreach(_.graphs)
    }
  }

  def getGraphs(predicate: T): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    predicates(predicate).graphs
  }

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    val pi = predicates(predicate)
    tripleItemPosition match {
      case TripleItemPosition.Subject(x) => pi.subjects(x).graphs
      case TripleItemPosition.Object(x) => pi.objects(x).graphs
      case _ => pi.graphs
    }
  }

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    predicates(predicate).subjects(subject).apply(`object`)
  }

  private def emptyMapWithGraphs = new MutableAnyWithGraphs[ItemMap[T], T](collectionsBuilder.emptyHashMap, collectionsBuilder.emptySet)

  private def emptySetWithGraphs = new MutableAnyWithGraphs[HashSet[T], T](collectionsBuilder.emptySet, collectionsBuilder.emptySet)

  private def addGraph(quad: Quad[T]): Unit = {
    //get predicate index by a specific predicate
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex[T]]]
      .getOrElseUpdate(quad.p, new TriplePredicateIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptyHashMap))
    //get predicate-subject index by a specific subject
    val psi = pi.subjects.asInstanceOf[M[AnyWithGraphs[ItemMap[T], T]]].getOrElseUpdate(quad.s, emptyMapWithGraphs).asInstanceOf[MutableAnyWithGraphs[ItemMap[T], T]]
    //add graph to this predicate-subject index - it is suitable for atom p(A, b) to enumerate all graphs
    psi.addGraph(quad.g)
    //get predicate-subject-object index by a specific object and add the graph
    // - it is suitable for enumerate all quads with graphs
    // - then construct Dataset from Index
    psi.value.asInstanceOf[M1].getOrElseUpdate(quad.o, collectionsBuilder.emptySet) += quad.g
    //get predicate-object index by a specific object and add the graph - it is suitable for atom p(a, B) to enumerate all graphs
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet[T], T]]].getOrElseUpdate(quad.o, emptySetWithGraphs).asInstanceOf[MutableAnyWithGraphs[HashSet[T], T]].addGraph(quad.g)
  }

  def addQuad(quad: Quad[T]): Unit = {
    if (graph.isEmpty) {
      if (severalGraphs) {
        addGraph(quad)
      } else {
        graph = Some(quad.g)
      }
    } else if (!graph.contains(quad.g)) {
      for {
        (p, m1) <- predicates.iterator
        (o, m2) <- m1.objects.iterator
        s <- m2.iterator
        g <- graph
      } {
        addGraph(new Quad(s, p, o, g))
      }
      addGraph(quad)
      severalGraphs = true
      graph = None
    }
    val si = subjects.asInstanceOf[M[TripleSubjectIndex[T]]].getOrElseUpdate(quad.s, new TripleSubjectIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptySet))
    val oi = objects.asInstanceOf[M[TripleObjectIndex[T]]].getOrElseUpdate(quad.o, new TripleObjectIndex(collectionsBuilder.emptySet, p => predicates(p).objects(quad.o).value.size))
    si.predicates.asInstanceOf[MutableHashSet[T]] += quad.p
    si.objects.asInstanceOf[M1].getOrElseUpdate(quad.o, collectionsBuilder.emptySet) += quad.p
    oi.predicates.asInstanceOf[MutableHashSet[T]] += quad.p
    val pi = predicates.asInstanceOf[M[TriplePredicateIndex[T]]].getOrElseUpdate(quad.p, new TriplePredicateIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptyHashMap))
    if (!severalGraphs) {
      pi.subjects.asInstanceOf[M[AnyWithGraphs[ItemMap[T], T]]]
        .getOrElseUpdate(quad.s, emptyMapWithGraphs).value.asInstanceOf[M1]
        .getOrElseUpdate(quad.o, collectionsBuilder.emptySet)
    }
    pi.objects.asInstanceOf[M[AnyWithGraphs[HashSet[T], T]]].getOrElseUpdate(quad.o, emptySetWithGraphs).value.asInstanceOf[MutableHashSet[T]] += quad.s
  }

  def trim(): Unit = {
    for (x <- subjects.valuesIterator) {
      for (x <- x.objects.valuesIterator) x.trim()
      x.predicates.trim()
      x.objects.trim()
    }
    for (x <- objects.valuesIterator) {
      x.predicates.trim()
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

  type ItemMap[T] = HashMap[T, HashSet[T]]
  type ItemMapWithGraphsAndSet[T] = HashMap[T, AnyWithGraphs[HashSet[T], T]]
  type ItemMapWithGraphsAndMap[T] = HashMap[T, AnyWithGraphs[ItemMap[T], T]]

  trait HashSet[T] {
    def iterator: Iterator[T]

    def apply(x: T): Boolean

    def size: Int

    def trim(): Unit

    def isEmpty: Boolean
  }

  trait MutableHashSet[T] extends HashSet[T] {
    def +=(x: T): Unit
  }

  trait HashMap[K, V] {
    def apply(key: K): V

    def keySet: HashSet[K]

    def get(key: K): Option[V]

    def keysIterator: Iterator[K]

    def valuesIterator: Iterator[V]

    def iterator: Iterator[(K, V)]

    def size: Int

    def isEmpty: Boolean

    def contains(key: K): Boolean

    def trim(): Unit
  }

  trait MutableHashMap[K, V] extends HashMap[K, V] {
    def getOrElseUpdate(key: K, default: => V): V
  }

  trait CollectionsBuilder[T] {
    def emptySet: MutableHashSet[T]

    def emptyHashMap[V]: MutableHashMap[T, V]
  }

  class AnyWithGraphs[T, G] private[TripleHashIndex](val value: T, val graphs: HashSet[G])

  class MutableAnyWithGraphs[T, G] private[TripleHashIndex](value: T, graphs: MutableHashSet[G]) extends AnyWithGraphs[T, G](value, graphs) {
    def addGraph(g: G): Unit = graphs += g
  }

  implicit def anyWithGraphsTo[T](hashSetWithGraphs: AnyWithGraphs[T, _]): T = hashSetWithGraphs.value

  class TriplePredicateIndex[T] private[TripleHashIndex](val subjects: ItemMapWithGraphsAndMap[T], val objects: ItemMapWithGraphsAndSet[T])(implicit collectionsBuilder: CollectionsBuilder[T]) {
    @volatile private var _size: Int = -1
    @volatile private var _graphs: Option[HashSet[T]] = None
    @volatile private var _leastFunctionalVariable: Option[ConceptPosition] = None

    def size: Int = {
      if (_size == -1) {
        _size = subjects.valuesIterator.map(_.size).sum
      }
      _size
    }

    def reset(): Unit = {
      _size = -1
      _graphs = None
      _leastFunctionalVariable = None
    }

    //add all graphs to this predicate index - it is suitable for atom p(a, b) to enumerate all graphs
    //it is contructed from all predicate-subject graphs
    def graphs: HashSet[T] = _graphs match {
      case Some(x) => x
      case None =>
        val set = collectionsBuilder.emptySet
        subjects.valuesIterator.flatMap(_.graphs.iterator).foreach(set += _)
        set.trim()
        _graphs = Some(set)
        set
    }

    /**
      * (C hasCitizen ?a), or (?a isCitizenOf C)
      * For this example C is the least functional variable
      */
    def leastFunctionalVariable: ConceptPosition = _leastFunctionalVariable match {
      case Some(x) => x
      case None =>
        val pos = if (functionality >= inverseFunctionality) {
          TriplePosition.Object
        } else {
          TriplePosition.Subject
        }
        _leastFunctionalVariable = Some(pos)
        pos
    }

    /**
      * (?a hasCitizen C), or (C isCitizenOf ?a)
      * For this example C is the most functional variable
      */
    def mostFunctionalVariable: ConceptPosition = if (leastFunctionalVariable == TriplePosition.Subject) {
      TriplePosition.Object
    } else {
      TriplePosition.Subject
    }

    def functionality: Double = subjects.size.toDouble / size

    def isFunction: Boolean = functionality == 1.0

    def isInverseFunction: Boolean = inverseFunctionality == 1.0

    def inverseFunctionality: Double = objects.size.toDouble / size
  }

  class TripleSubjectIndex[T] private[TripleHashIndex](val objects: ItemMap[T], val predicates: HashSet[T]) {
    @volatile private var _size: Int = -1

    def reset(): Unit = _size = -1

    def size: Int = {
      if (_size == -1) {
        _size = objects.valuesIterator.map(_.size).sum
      }
      _size
    }
  }

  class TripleObjectIndex[T] private[TripleHashIndex](val predicates: HashSet[T], computePredicateObjectSize: T => Int) {
    @volatile private var _size: Int = -1

    def reset(): Unit = _size = -1

    def size: Int = {
      if (_size == -1) {
        _size = predicates.iterator.map(computePredicateObjectSize(_)).sum
      }
      _size
    }
  }

  class Quad[T](val s: T, val p: T, val o: T, val g: T)

  def apply[T](quads: Traversable[Quad[T]])(implicit debugger: Debugger, collectionsBuilder: CollectionsBuilder[T]): TripleHashIndex[T] = {
    val index = new TripleHashIndex[T]
    debugger.debug("Dataset indexing") { ad =>
      for (quad <- quads) {
        index.addQuad(quad)
        ad.done()
      }
      index.trim()
    }
    index
  }

}