package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.data.{DiscretizationTask, Quad, TripleItem}
import com.github.propi.rdfrules.experiments.IndexOps.DiscretizedTree
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.rule.Threshold.{MinHeadCoverage, MinHeadSize}
import com.github.propi.rdfrules.utils.{Debugger, ForEach}
import eu.easyminer.discretization.Consumer
import eu.easyminer.discretization.algorithm.Discretization
import eu.easyminer.discretization.impl.sorting.SortedInMemoryNumericProducer
import eu.easyminer.discretization.impl.{Interval, Producer}

/**
  * Created by Vaclav Zeman on 23. 3. 2020.
  */
class IndexOps private(implicit mapper: TripleItemIndex, thi: TripleIndex[Int]) {

  private def toProducer[T](x: ForEach[T]): Producer[T] = (consumer: Consumer[T]) => x.foreach(consumer.consume)

  private def buildDiscretizedTree(intervals: IndexedSeq[Interval], arity: Int): DiscretizedTree = {
    val levels = ((math.log((arity - 1) * intervals.length + 1) / math.log(arity)) - 1).toInt
    val reversedIt = intervals.reverseIterator.collect {
      case x: Interval.WithFrequency => x
    }

    @scala.annotation.tailrec
    def buildLevel(level: Int, reversedChildren: Seq[DiscretizedTree]): DiscretizedTree = {
      if (reversedIt.hasNext) {
        val intervalsPerLevel = math.pow(arity, level).toInt
        if (reversedChildren.isEmpty) {
          buildLevel(level - 1, reversedIt.take(intervalsPerLevel).map(DiscretizedTree.Leaf).toList)
        } else {
          buildLevel(level - 1, reversedIt.take(intervalsPerLevel).zip(reversedChildren.grouped(arity)).map {
            case (interval, children) => DiscretizedTree.Node(interval, children)
          }.toList)
        }
      } else {
        reversedChildren.head
      }
    }

    buildLevel(levels, Nil)
  }

  def discretizedTreeQuads(predicate: TripleItem.Uri, minSupportUpper: Int, discretizedTree: DiscretizedTree)(implicit debugger: Debugger): ForEach[Quad] = (f: Quad => Unit) => {
    def isCutOff(tree: DiscretizedTree): Boolean = tree match {
      case DiscretizedTree.Node(_, children) => children.nonEmpty && children.forall(_.interval.frequency >= minSupportUpper)
      case _ => false
    }

    val predicateQuads: ForEach[Quad] = {
      val predicateIndex = mapper.getIndexOpt(predicate).map(x => x -> thi.predicates(x))
      (f: Quad => Unit) => {
        for {
          (p, predicateIndex) <- predicateIndex
          g = thi.getGraphs(p).iterator.next()
          (s, objects) <- predicateIndex.subjects.pairIterator
          o <- objects.iterator
        } {
          f(IndexItem.Quad(s, p, o, g).toQuad)
        }
      }
    }
    val buildPredicate: String => TripleItem.Uri = predicate match {
      case TripleItem.LongUri(uri) => suffix => TripleItem.LongUri(uri + suffix)
      case x: TripleItem.PrefixedUri => suffix => x.copy(localName = x.localName + suffix)
      case TripleItem.BlankNode(id) => suffix => TripleItem.BlankNode(id + suffix)
    }

    @scala.annotation.tailrec
    def addLevelToIndex(level: Int, intervals: Iterable[DiscretizedTree]): Unit = {
      if (intervals.nonEmpty) {
        if (level > 0) {
          val suffix = "_discretized_level_" + level
          val newPredicate = buildPredicate(suffix)
          val cutOffIntervals = intervals.filter(!isCutOff(_))
          for {
            quad <- predicateQuads
            objectNumber <- Option(quad.triple.`object`).collect {
              case TripleItem.NumberDouble(value) => value
            }
            interval <- cutOffIntervals.iterator.map(_.interval).find(_.isInInterval(objectNumber))
          } {
            f(Quad(data.Triple(quad.triple.subject, newPredicate, TripleItem.Interval(interval)), quad.graph))
          }
        }
        val allChildren = intervals.iterator.collect {
          case x: DiscretizedTree.Node => x.children
        }.flatten.toSet
        addLevelToIndex(level + 1, allChildren)
      }
    }

    addLevelToIndex(0, List(discretizedTree))
  }

  private def removeDuplicitIntervals(tree: DiscretizedTree): DiscretizedTree = tree match {
    case DiscretizedTree.Node(interval, children) =>
      val filteredChildren = children.collect {
        case child@DiscretizedTree.Node(interval2, _) if interval != interval2 => removeDuplicitIntervals(child)
        case child@DiscretizedTree.Leaf(interval2) if interval != interval2 => child
      }
      if (filteredChildren.isEmpty) {
        DiscretizedTree.Leaf(interval)
      } else {
        DiscretizedTree.Node(interval, filteredChildren)
      }
    case _: DiscretizedTree.Leaf => tree
  }

  def getDiscretizedTrees(predicates: Iterator[TripleItem], minSupports: Map[Int, Int], arity: Int): Iterator[(TripleItem, DiscretizedTree)] = {
    for {
      predicate <- predicates
      predicateId <- mapper.getIndexOpt(predicate)
      tpi <- thi.predicates.get(predicateId)
    } yield {
      val task = DiscretizationTask.EquisizeTree(minSupports(predicateId), arity)
      val discretization = Discretization[Double](task)
      val col = new ForEach[Double] {
        def foreach(f: Double => Unit): Unit = {
          tpi.objects.iterator.map(mapper.getTripleItem).collect {
            case TripleItem.NumberDouble(value) => value
          }.foreach(f)
        }
      }
      val tree = buildDiscretizedTree(discretization.discretize(SortedInMemoryNumericProducer(toProducer(col), task.buffer)), arity)
      predicate -> removeDuplicitIntervals(tree)
    }
  }

  def getNumericPredicates(predicates: Iterator[TripleItem], minSupports: Map[Int, Int]): Iterator[(TripleItem, Int)] = {
    (if (predicates.nonEmpty) predicates.flatMap(mapper.getIndexOpt) else thi.predicates.iterator).map { predicate =>
      mapper.getTripleItem(predicate) -> thi.predicates.get(predicate).iterator.flatMap(_.objects.pairIterator).filter(x => mapper.getTripleItem(x._1).isInstanceOf[TripleItem.Number[_]]).map(_._2.size).sum
    }.filter(x => x._2 >= minSupports(mapper.getIndex(x._1)))
  }

  /*private def getPredicateNeighbours(predicate: Int, hops: Int, minHeadSize: MinHeadSize): Iterator[Int] = {
    val pindex = thi.predicates(predicate)
    val p = Iterator(predicate).filter(_ => pindex.size(true) >= minHeadSize.value)
    if (hops > 0) {
      (pindex.neighboursSO.iterator ++ pindex.neighboursSS.iterator ++ pindex.neighboursOS.iterator ++ pindex.neighboursOO.iterator)
        .distinct
        .flatMap(getPredicateNeighbours(_, hops - 1, minHeadSize)) ++ p
    } else {
      p
    }
  }*/

  private def getPredicateMinSupportLower(predicate: Int, hops: Int, minHeadSize: MinHeadSize): Option[Int] = {
    val pindex = thi.predicates(predicate)
    val psize = Some(pindex.size(true)).filter(_ >= minHeadSize.value)
    if (hops > 0) {
      ((pindex.neighboursSO.iterator ++ pindex.neighboursSS.iterator ++ pindex.neighboursOS.iterator ++ pindex.neighboursOO.iterator)
        .distinct
        .flatMap(getPredicateMinSupportLower(_, hops - 1, minHeadSize)) ++ psize.iterator).minOption
    } else {
      psize
    }
  }

  def getMinSupportLower(minHeadSize: MinHeadSize, minHeadCoverage: MinHeadCoverage, maxRuleLength: Int): Map[Int, Int] = {
    thi.predicates.iterator.map(p => p -> getPredicateMinSupportLower(p, math.max(maxRuleLength - 2, 1), minHeadSize).map(x => math.ceil(x * minHeadCoverage.value).toInt).getOrElse(Int.MaxValue)).toMap
  }

  private def getPredicateMinSupportUpper(predicate: Int, hops: Int, minHeadSize: MinHeadSize): Option[Int] = {
    val pindex = thi.predicates(predicate)
    val psize = Some(pindex.size(true)).filter(_ >= minHeadSize.value)
    if (hops > 0) {
      ((pindex.neighboursSO.iterator ++ pindex.neighboursSS.iterator ++ pindex.neighboursOS.iterator ++ pindex.neighboursOO.iterator)
        .distinct
        .flatMap(getPredicateMinSupportUpper(_, hops - 1, minHeadSize)) ++ psize.iterator).maxOption
    } else {
      psize
    }
  }

  def getMinSupportUpper(minHeadSize: MinHeadSize, minHeadCoverage: MinHeadCoverage, maxRuleLength: Int): Map[Int, Int] = {
    thi.predicates.iterator.map(p => p -> getPredicateMinSupportUpper(p, math.max(maxRuleLength - 2, 1), minHeadSize).map(x => math.ceil(x * minHeadCoverage.value).toInt).getOrElse(Int.MinValue)).toMap
  }

}

object IndexOps {

  sealed trait DiscretizedTree {
    val interval: Interval.WithFrequency
  }

  object DiscretizedTree {

    case class Node(interval: Interval.WithFrequency, children: Seq[DiscretizedTree]) extends DiscretizedTree {
      override def toString: String = TripleItem.Interval(interval).toString + "\n" + children.mkString("\n").replaceAll("(?m)^", " -> ")
    }

    case class Leaf(interval: Interval.WithFrequency) extends DiscretizedTree {
      override def toString: String = TripleItem.Interval(interval).toString
    }

  }

  implicit class PimpedIndex(val index: Index) extends AnyVal {
    def useRichOps[T](f: IndexOps => T): T = {
      f(new IndexOps()(index.tripleItemMap, index.tripleMap))
    }
  }

}
