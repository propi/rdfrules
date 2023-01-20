package com.github.propi.rdfrules.algorithm.clustering

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.utils.Debugger

class HierarchicalClustering[T] private(arity: Int, simThreshold: Double, deepSim: Boolean)(implicit sim: SimilarityCounting[T], debugger: Debugger) extends Clustering[T] {

  private trait Node {
    private val _children = new Array[Cluster](arity)

    final def clusters: Iterator[Iterator[T]] = _children.iterator.filter(_ != null).flatMap(x => Iterator(x.clusterElems) ++ x.clusters)

    final def addToChildren(elem: T): Unit = {
      val (i, maxsim) = _children.indices.iterator.map { i =>
        val child = _children(i)
        if (child == null) {
          i -> simThreshold
        } else {
          i -> child.similarity(elem)
        }
      }.maxBy(_._2)
      val child = _children(i)
      if (child == null) {
        _children(i) = new Cluster(elem)
      } else if (maxsim >= simThreshold) {
        child.addToCluster(elem)
      } else {
        child.addToChildren(elem)
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
  def clusters(data: IndexedSeq[T]): IndexedSeq[IndexedSeq[T]] = {
    val root = new Root
    debugger.debug("Hierarchical clustering process", data.size) { implicit ad =>
      for (elem <- data) {
        root.addToChildren(elem)
        ad.done()
      }
    }
    root.clusters.map(_.toIndexedSeq).toIndexedSeq
  }
}

object HierarchicalClustering {

  def apply[T](arity: Int = 2, simThreshold: Double = 0.8, deepSim: Boolean = false)(implicit sim: SimilarityCounting[T], debugger: Debugger): HierarchicalClustering[T] = new HierarchicalClustering(arity, simThreshold, deepSim)(sim, debugger)

}