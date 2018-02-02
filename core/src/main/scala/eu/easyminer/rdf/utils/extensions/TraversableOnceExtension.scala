package eu.easyminer.rdf.utils.extensions

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
object TraversableOnceExtension {

  implicit class PimpedTraversableOnce[T](col: TraversableOnce[T]) {

    def distinct: Traversable[T] = new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        val set = collection.mutable.HashSet.empty[T]
        for (x <- col if !set(x)) {
          set += x
          f(x)
        }
      }
    }

    def distinctBy[A](f: T => A): Traversable[T] = new Traversable[T] {
      def foreach[U](g: T => U): Unit = {
        val set = collection.mutable.HashSet.empty[A]
        for (x <- col; y = f(x); if !set(f(x))) {
          set += y
          g(x)
        }
      }
    }

    def concat(col2: TraversableOnce[T]): Traversable[T] = new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        col foreach f
        col2 foreach f
      }
    }

  }

}