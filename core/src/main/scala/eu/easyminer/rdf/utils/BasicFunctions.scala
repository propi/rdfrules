package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
object BasicFunctions {

  object Match {
    def default: PartialFunction[Any, Unit] = {
      case _ =>
    }

    def apply[T](x: T)(body: PartialFunction[T, Unit]): Unit = (body orElse default) (x)
  }

}
