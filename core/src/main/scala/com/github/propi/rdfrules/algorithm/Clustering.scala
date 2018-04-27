package com.github.propi.rdfrules.algorithm

/**
  * Created by Vaclav Zeman on 27. 4. 2018.
  *
  * Abstraction for a clustering algorithm
  */
trait Clustering[T] {

  /**
    * Make clusters from indexed sequence
    *
    * @param data indexed sequence of data
    * @return clustered data
    */
  def clusters(data: IndexedSeq[T]): IndexedSeq[IndexedSeq[T]]
}
