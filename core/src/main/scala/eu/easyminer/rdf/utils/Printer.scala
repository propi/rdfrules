package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait Printer[T] {

  def print(v: T): Unit

  def printBatch(coll: Traversable[T])(offset: Int, limit: Int): Unit = coll.view.slice(offset, offset + limit).foreach(print)

}

object Printer {

  def apply[T](implicit dataPrinter: Printer[T]): Printer[T] = dataPrinter

}
