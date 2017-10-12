package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 11. 10. 2017.
  */
class OptionalLazyVal[T] private(f: => T, isLazy: Boolean) {

  private lazy val x = if (isLazy) None else Some(f)

  def apply[U](g: T => U): U = {
    val v = x match {
      case Some(x) => x
      case None => f
    }
    g(v)
  }

}

object OptionalLazyVal {

  def apply[T](isLazy: Boolean)(f: => T): OptionalLazyVal[T] = new OptionalLazyVal(f, isLazy)

}
