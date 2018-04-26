package com.github.propi.rdfrules.algorithm.dbscan

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
class DbScan[T] private(minNeighbours: Int, minSimilarity: Double, data: Seq[T])(implicit similarity: (T, T) => Double) {

  private def searchReachables(point: T, data: Seq[T]) = data.par.partition(similarity(point, _) >= minSimilarity)

  @scala.annotation.tailrec
  private def makeCluster(remainingPoints: Seq[T], cluster: Seq[T], nonCluster: Seq[T]): (Seq[T], Seq[T]) = if (remainingPoints.isEmpty) {
    (cluster, nonCluster)
  } else if (nonCluster.isEmpty) {
    (cluster ++ remainingPoints, nonCluster)
  } else {
    val point = remainingPoints.head
    val (nonClusterReachable, nonClusterOthers) = searchReachables(point, nonCluster)
    if (nonClusterReachable.size >= minNeighbours || (searchReachables(point, cluster ++ remainingPoints.tail)._1.size + nonClusterReachable.size) >= minNeighbours) {
      makeCluster(remainingPoints.tail ++ nonClusterReachable, point +: cluster, nonClusterOthers.seq)
    } else {
      makeCluster(remainingPoints.tail, point +: cluster, nonCluster)
    }
  }

  @scala.annotation.tailrec
  private def makeClusters(nonCluster: Seq[T], clusters: Seq[Seq[T]]): Seq[Seq[T]] = if (nonCluster.isEmpty) {
    clusters
  } else {
    val (cluster, others) = makeCluster(Vector(nonCluster.head), Vector.empty, nonCluster.tail)
    makeClusters(others, cluster +: clusters)
  }

  /**
    * TODO - make it faster!
    *
    * @return
    */
  def clusters: Seq[Seq[T]] = makeClusters(data.toVector, Vector.empty)

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

  def apply[T](minNeighbours: Int, minSimilarity: Double, data: Seq[T])(implicit similarity: (T, T) => Double): Seq[Seq[T]] = new DbScan(minNeighbours, minSimilarity, data).clusters

}