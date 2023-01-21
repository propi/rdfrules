package com.github.propi.rdfrules.algorithm.clustering

import java.util.concurrent.ForkJoinPool
import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.Debugger.ActionDebugger

import scala.collection.parallel.CollectionConverters.ImmutableSeqIsParallelizable
import scala.collection.parallel.ForkJoinTaskSupport

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
class DbScan[T] private(minNeighbours: Int, minSimilarity: Double, parallelism: Int)(implicit similarity: SimilarityCounting[T], debugger: Debugger) extends Clustering[T] {

  private def searchReachables(point: T, data: Seq[T]) = {
    val parSeq = data.par
    parSeq.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(parallelism))
    parSeq.partition(similarity(point, _) >= minSimilarity)
  }

  @scala.annotation.tailrec
  private def makeCluster(remainingPoints: IndexedSeq[T], cluster: IndexedSeq[T], nonCluster: Seq[T])(implicit ad: ActionDebugger): (IndexedSeq[T], Seq[T]) = if (remainingPoints.isEmpty) {
    (cluster, nonCluster)
  } else if (nonCluster.isEmpty) {
    (cluster ++ remainingPoints, nonCluster)
  } else {
    if (debugger.isInterrupted) {
      (remainingPoints.iterator ++ nonCluster.iterator).foreach(_ => ad.done())
      (cluster ++ remainingPoints ++ nonCluster, Nil)
    } else {
      ad.done()
      val point = remainingPoints.head
      val (nonClusterReachable, nonClusterOthers) = searchReachables(point, nonCluster)
      if (nonClusterReachable.size >= minNeighbours || (searchReachables(point, cluster ++ remainingPoints.tail)._1.size + nonClusterReachable.size) >= minNeighbours) {
        makeCluster(remainingPoints.tail ++ nonClusterReachable, point +: cluster, nonClusterOthers.seq)
      } else {
        makeCluster(remainingPoints.tail, point +: cluster, nonCluster)
      }
    }
  }

  @scala.annotation.tailrec
  private def makeClusters(nonCluster: Seq[T], clusters: IndexedSeq[IndexedSeq[T]])(implicit ad: ActionDebugger): IndexedSeq[IndexedSeq[T]] = if (nonCluster.isEmpty) {
    clusters
  } else {
    val (cluster, others) = makeCluster(Vector(nonCluster.head), Vector.empty, nonCluster.tail)
    makeClusters(others, cluster +: clusters)
  }

  /**
    * Make clusters from indexed sequence
    * TODO - make it faster!
    *
    * @param data indexed sequence of data
    * @return clustered data
    */
  def clusters(data: IndexedSeq[T], taskName: String): IndexedSeq[IndexedSeq[T]] = debugger.debug(s"DBscan clustering process${if (taskName.isEmpty) "" else s" for $taskName"}", data.size, true) { implicit ad =>
    ad.done()
    makeClusters(data, Vector.empty)
  }

  //it is slower than immutable version!
  /*def mutableClusters: Seq[collection.Set[T]] = {
    val clusters = collection.mutable.ListBuffer.empty[collection.Set[T]]
    val nonCluster = collection.mutable.Set(data: _*)
    while (nonCluster.nonEmpty) {
      val point = nonCluster.head
      val cluster = collection.mutable.Set.empty[T]
      val nonClusterReachable = collection.mutable.Set(point)
      while (nonClusterReachable.nonEmpty) {
        val expandablePoint = nonClusterReachable.head
        nonClusterReachable -= expandablePoint
        cluster += expandablePoint
        val reachable = nonCluster.par.filter(similarity(expandablePoint, _) >= minSimilarity).seq
        if (reachable.size > minNeighbours) {
          for (x <- reachable if !cluster(x)) nonClusterReachable += x
        }
      }
      clusters += cluster
      nonCluster --= cluster
    }
    clusters
  }*/

}

object DbScan {

  def apply[T](minNeighbours: Int = 5, minSimilarity: Double = 0.9, parallelism: Int = Runtime.getRuntime.availableProcessors())(implicit similarity: SimilarityCounting[T], debugger: Debugger): Clustering[T] = new DbScan(minNeighbours, minSimilarity, parallelism)

}