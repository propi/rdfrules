package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait DataPrinter[T] {

  def print(value: T): Unit

  def printBatch(coll: Traversable[T])(offset: Int, limit: Int): Unit = coll.view.slice(offset, offset + limit).foreach(print)

}

object DataPrinter {

  implicit val tripleDataPrinter: DataPrinter[Triple] = (value: Triple) => println(value.subject + "  " + value.predicate + "  " + value.`object`)

  def apply[T](implicit dataPrinter: DataPrinter[T]): DataPrinter[T] = dataPrinter

}
