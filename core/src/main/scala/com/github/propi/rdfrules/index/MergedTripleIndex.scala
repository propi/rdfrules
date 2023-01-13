package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.index.IndexCollections.{HashMap, HashSet, MergedHashMap, MergedHashSet, Reflexiveable}
import com.github.propi.rdfrules.index.TripleIndex.{ObjectIndex, PredicateIndex, SubjectIndex}
import com.github.propi.rdfrules.rule.TripleItemPosition

class MergedTripleIndex[T] private(index1: TripleIndex[T], index2: TripleIndex[T]) extends TripleIndex[T] {

  root =>

  def getGraphs(predicate: T): HashSet[T] = new MergedHashSet[T](index1.getGraphs(predicate), index2.getGraphs(predicate))

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T] = new MergedHashSet[T](index1.getGraphs(predicate, tripleItemPosition), index2.getGraphs(predicate, tripleItemPosition))

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T] = new MergedHashSet[T](index1.getGraphs(subject, predicate, `object`), index2.getGraphs(subject, predicate, `object`))

  def size(nonReflexive: Boolean): Int = index1.size(nonReflexive) + index2.size(nonReflexive)

  private class MergedHashSetReflexivable(hset1: HashSet[T] with Reflexiveable, hset2: HashSet[T] with Reflexiveable) extends MergedHashSet(hset1, hset2) with Reflexiveable {
    def hasReflexiveRecord: Boolean = hset1.hasReflexiveRecord || hset2.hasReflexiveRecord

    override def size(nonReflexive: Boolean): Int = super.size - (if (hset1.hasReflexiveRecord) 1 else 0) - (if (hset2.hasReflexiveRecord) 1 else 0)
  }

  private class MergedPredicateIndex(pi1: PredicateIndex[T], pi2: PredicateIndex[T]) extends PredicateIndex[T] {
    def buildFastIntMap(from: Iterator[(T, Int)]): HashMap[T, Int] = pi1.buildFastIntMap(from)

    def context: TripleIndex[T] = root

    val subjects: HashMap[T, HashSet[T] with IndexCollections.Reflexiveable] = new MergedHashMap(pi1.subjects, pi2.subjects)(_ => (v1, v2) => new MergedHashSetReflexivable(v1, v2))

    val objects: HashMap[T, HashSet[T] with IndexCollections.Reflexiveable] = new MergedHashMap(pi1.objects, pi2.objects)(_ => (v1, v2) => new MergedHashSetReflexivable(v1, v2))

    private lazy val _size: Int = subjects.valuesIterator.map(_.size).sum
    private lazy val _nonReflexiveSize: Int = subjects.valuesIterator.map(_.size(true)).sum

    def size(nonReflexive: Boolean): Int = {
      if (nonReflexive) {
        _nonReflexiveSize
      } else {
        _size
      }
    }
  }

  private class MergedSubjectIndex(si1: SubjectIndex[T], si2: SubjectIndex[T]) extends SubjectIndex[T] {
    val predicates: HashSet[T] = new MergedHashSet[T](si1.predicates, si2.predicates)

    val objects: HashMap[T, HashSet[T] with IndexCollections.Reflexiveable] = new MergedHashMap(si1.objects, si2.objects)(_ => (v1, v2) => new MergedHashSetReflexivable(v1, v2))

    private lazy val _size: Int = objects.valuesIterator.map(_.size).sum
    private lazy val _nonReflexiveSize: Int = objects.valuesIterator.map(_.size(true)).sum

    def size(nonReflexive: Boolean): Int = {
      if (nonReflexive) {
        _nonReflexiveSize
      } else {
        _size
      }
    }
  }

  private class MergedObjectIndex(oi1: ObjectIndex[T], oi2: ObjectIndex[T], computePredicateObjectSize: (T, Boolean) => Int) extends ObjectIndex[T] {
    val predicates: HashSet[T] = new MergedHashSet[T](oi1.predicates, oi2.predicates)

    private lazy val _size: Int = predicates.iterator.map(computePredicateObjectSize(_, false)).sum
    private lazy val _nonReflexiveSize: Int = predicates.iterator.map(computePredicateObjectSize(_, true)).sum

    def size(nonReflexive: Boolean): Int = {
      if (nonReflexive) {
        _nonReflexiveSize
      } else {
        _size
      }
    }
  }

  lazy val predicates: HashMap[T, PredicateIndex[T]] = {
    val merged = collection.mutable.HashMap.from(
      index1.predicates.pairIterator.flatMap(x => index2.predicates.get(x._1).map(y => x._1 -> new MergedPredicateIndex(x._2, y)))
    )
    new MergedHashMap(index1.predicates, index2.predicates)(k => (_, _) => merged(k))
  }

  lazy val subjects: HashMap[T, SubjectIndex[T]] = {
    val merged = collection.mutable.HashMap.from(
      index1.subjects.pairIterator.flatMap(x => index2.subjects.get(x._1).map(y => x._1 -> new MergedSubjectIndex(x._2, y)))
    )
    new MergedHashMap(index1.subjects, index2.subjects)(k => (_, _) => merged(k))
  }

  lazy val objects: HashMap[T, ObjectIndex[T]] = {
    val merged = collection.mutable.HashMap.from(
      index1.objects.pairIterator.flatMap(x => index2.objects.get(x._1).map(y => x._1 -> new MergedObjectIndex(x._2, y, (p, nonReflexive) => {
        root.predicates(p).objects(x._1).size(nonReflexive)
      })))
    )
    new MergedHashMap(index1.objects, index2.objects)(k => (_, _) => merged(k))
  }

  def quads: Iterator[IndexItem.Quad[T]] = index1.quads ++ index2.quads

  def evaluateAllLazyVals(): Unit = {
    index1.evaluateAllLazyVals()
    index2.evaluateAllLazyVals()
  }

}

object MergedTripleIndex {

  def apply[T](index1: TripleIndex[T], index2: TripleIndex[T]): MergedTripleIndex[T] = new MergedTripleIndex(index1, index2)

}