package eu.easyminer.rdf.clustering

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
class DbScan[T] private(minPts: Int, eps: Double, data: Iterable[T])(implicit similarity: (T, T) => Double) {

  private def searchReachables(point: T, data: Iterable[T]) = data.partition(similarity(point, _) >= eps)

  @scala.annotation.tailrec
  private def makeCluster(remainingPoints: Iterable[T], cluster: Seq[T], nonCluster: Iterable[T]): (Iterable[T], Iterable[T]) = if (remainingPoints.isEmpty) {
    (cluster, nonCluster)
  } else if (nonCluster.isEmpty) {
    (cluster ++ remainingPoints, nonCluster)
  } else {
    val point = remainingPoints.head
    val (nonClusterReachable, nonClusterOthers) = searchReachables(point, nonCluster)
    if (nonClusterReachable.size >= minPts || (searchReachables(point, cluster ++ remainingPoints.tail)._1.size + nonClusterReachable.size) >= minPts) {
      makeCluster(remainingPoints.tail ++ nonClusterReachable, point +: cluster, nonClusterOthers)
    } else {
      makeCluster(remainingPoints.tail, point +: cluster, nonCluster)
    }
  }

  @scala.annotation.tailrec
  private def makeClusters(nonCluster: Iterable[T], clusters: Seq[Iterable[T]]): Iterable[Iterable[T]] = if (nonCluster.isEmpty) {
    clusters
  } else {
    val (cluster, others) = makeCluster(List(nonCluster.head), Nil, nonCluster.tail)
    makeClusters(others, cluster +: clusters)
  }

  def clusters = makeClusters(data, Nil)

}

object DbScan {

  def apply[T](minPts: Int, eps: Double, data: Iterable[T])(implicit similarity: (T, T) => Double) = new DbScan(minPts, eps, data).clusters

}
