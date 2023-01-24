package com.github.propi.rdfrules.algorithm.clustering

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.utils.Debugger

import scala.annotation.tailrec

class TreeBasedDbScan[T] private(arity: Int, simThreshold: Double, deepSim: Boolean)(implicit sim: SimilarityCounting[T], debugger: Debugger) extends Clustering[T] {

  private trait Node {
    private val _children = new Array[Cluster](arity)

    final def children: Iterator[Cluster] = _children.iterator.filter(_ != null)

    final def clusters: Iterator[Iterator[T]] = {
      val stack = collection.mutable.Stack(children)

      new Iterator[Iterator[T]] {
        def hasNext: Boolean = stack.nonEmpty && stack.top.hasNext

        def next(): Iterator[T] = {
          val topIt = stack.top
          val nextCluster = topIt.next()
          val res = nextCluster.clusterElems
          if (!topIt.hasNext) stack.pop()
          val nextLevel = nextCluster.children
          if (nextLevel.hasNext) stack.push(nextLevel)
          res
        }
      }
    }

    final def addToChildren(elem: T): Unit = Node.recAddToChildren(elem, this, 0)
  }

  private object Node {
    @tailrec
    private def recAddToChildren(elem: T, node: Node, depth: Int): Unit = {
      val (i, maxsim) = node._children.indices.iterator.map { i =>
        val child = node._children(i)
        if (child == null) {
          i -> simThreshold
        } else {
          i -> child.similarity(elem)
        }
      }.maxBy(_._2)
      val child = node._children(i)
      if (child == null) {
        node._children(i) = new Cluster(elem)
      } else if (maxsim >= simThreshold) {
        child.addToCluster(elem)
      } else {
        recAddToChildren(elem, child, depth + 1)
      }
    }
  }

  private class Root extends Node

  private class Cluster(val mainElem: T) extends Node {
    private val _elems = collection.mutable.ArrayBuffer.empty[T]

    def clusterElems: Iterator[T] = Iterator(mainElem) ++ _elems.iterator

    def similarity(elem: T): Double = if (deepSim && _elems.nonEmpty) math.max(sim(mainElem, elem), _elems.iterator.map(sim(_, elem)).max) else sim(mainElem, elem)

    def addToCluster(elem: T): Unit = _elems.addOne(elem)
  }

  /**
    * Make clusters from indexed sequence
    *
    * @param data indexed sequence of data
    * @return clustered data
    */
  def clusters(data: IndexedSeq[T], taskName: String): IndexedSeq[IndexedSeq[T]] = {
    val root = new Root
    debugger.debug(s"Hierarchical clustering process${if (taskName.isEmpty) "" else s" for $taskName"}", data.size, true) { implicit ad =>
      for (elem <- data) {
        root.addToChildren(elem)
        ad.done()
      }
    }
    root.clusters.map(_.toIndexedSeq).toIndexedSeq
  }
}

object TreeBasedDbScan {

  def apply[T](arity: Int = 2, simThreshold: Double = 0.8, deepSim: Boolean = false)(implicit sim: SimilarityCounting[T], debugger: Debugger): Clustering[T] = new TreeBasedDbScan(arity, simThreshold, deepSim)(sim, debugger)

}