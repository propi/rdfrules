package com.github.propi.rdfrules.algorithm.dbscan

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
class DbScan[T] private(minNeighbours: Int, minSimilarity: Double, data: Seq[T])(implicit similarity: (T, T) => Double) {

  private def searchReachables(point: T, data: Seq[T]) = data.partition(similarity(point, _) >= minSimilarity)

  @scala.annotation.tailrec
  private def makeCluster(remainingPoints: Seq[T], cluster: Seq[T], nonCluster: Seq[T]): (Seq[T], Seq[T]) = if (remainingPoints.isEmpty) {
    (cluster, nonCluster)
  } else if (nonCluster.isEmpty) {
    (cluster ++ remainingPoints, nonCluster)
  } else {
    val point = remainingPoints.head
    val (nonClusterReachable, nonClusterOthers) = searchReachables(point, nonCluster)
    if (nonClusterReachable.size >= minNeighbours || (searchReachables(point, cluster ++ remainingPoints.tail)._1.size + nonClusterReachable.size) >= minNeighbours) {
      makeCluster(remainingPoints.tail ++ nonClusterReachable, point +: cluster, nonClusterOthers)
    } else {
      makeCluster(remainingPoints.tail, point +: cluster, nonCluster)
    }
  }

  @scala.annotation.tailrec
  private def makeClusters(nonCluster: Seq[T], clusters: Seq[Seq[T]]): Seq[Seq[T]] = if (nonCluster.isEmpty) {
    clusters
  } else {
    val (cluster, others) = makeCluster(List(nonCluster.head), Nil, nonCluster.tail)
    makeClusters(others, cluster +: clusters)
  }

  def clusters: Seq[Seq[T]] = makeClusters(data, Nil)

}

object DbScan {

  def apply[T](minNeighbours: Int, minSimilarity: Double, data: Seq[T])(implicit similarity: (T, T) => Double): Seq[Seq[T]] = new DbScan(minNeighbours, minSimilarity, data).clusters

}
