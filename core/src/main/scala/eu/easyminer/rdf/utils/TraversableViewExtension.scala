package eu.easyminer.rdf.utils

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
object TraversableViewExtension {

  implicit class PimpedTraversableView[T, Coll](coll: TraversableView[T, Coll]) {

    def sliceDistinct(from: Int, until: Int): TraversableView[T, Coll] = new coll.Transformed[T] {
      def foreach[U](f: (T) => U): Unit = {
        val set = collection.mutable.Set.empty[T]
        coll.foreach { x =>
          if (set.size < until) {
            if (!set(x)) {
              set += x
              if (set.size > from) f(x)
            }
          }
        }
      }
    }

  }

}
