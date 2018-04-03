package com.github.propi.rdfrules.printer

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait Printer[T] {

  def println(v: T): Unit

  def printBatch(coll: Traversable[T])(offset: Int, limit: Int): Unit = coll.view.slice(offset, offset + limit).foreach(println)

}

object Printer {

  def apply[T](implicit dataPrinter: Printer[T]): Printer[T] = dataPrinter

}
