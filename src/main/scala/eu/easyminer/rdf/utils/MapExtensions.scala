package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 29. 7. 2017.
  */
object MapExtensions {

  implicit class PimpedMapChain[A, B](mapChain: Option[Map[A, B]]) {
    def apply(k: A): Option[B] = mapChain.flatMap(_.get(k))
  }

}
