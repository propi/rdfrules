package eu.easyminer.rdf.utils.serialization

/**
  * Created by Vaclav Zeman on 1. 8. 2017.
  */
trait SerializationSize[T] {

  val size: Int

}

object SerializationSize {

  implicit def default[T]: SerializationSize[T] = new SerializationSize[T] {
    val size: Int = 0
  }

}