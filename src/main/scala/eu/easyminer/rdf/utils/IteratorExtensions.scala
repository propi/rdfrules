package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 30. 6. 2017.
  */
object IteratorExtensions {

  implicit class PimpedIterator[T](it: Iterator[T]) {
    def distinctBy[A](f: T => A): Iterator[T] = new Iterator[T] {
      private val walkedItems = collection.mutable.HashSet.empty[A]
      private var c = Option.empty[T]

      def hasNext: Boolean = {
        while (it.hasNext && c.isEmpty) {
          val item = it.next()
          val key = f(item)
          if (!walkedItems(key)) {
            walkedItems += key
            c = Some(item)
          }
        }
        if (c.isEmpty) walkedItems.clear()
        c.isDefined
      }

      def next(): T = if (hasNext) {
        val x = c.get
        c = None
        x
      } else {
        Iterator.empty.next()
      }
    }
  }

}
