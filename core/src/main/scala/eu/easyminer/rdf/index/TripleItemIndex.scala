package eu.easyminer.rdf.index

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.data.TripleItemSerialization._
import eu.easyminer.rdf.data.{Dataset, Graph, TripleItem}

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleItemIndex extends Index[TripleItem] {

  def save(graph: Graph, buildOutputStream: => OutputStream): Unit = save(Dataset(graph), buildOutputStream)

  def save(dataset: Dataset, buildOutputStream: => OutputStream): Unit = save(dataset.toTriples.flatMap(triple => List(triple.subject, triple.predicate, triple.`object`)).toSet, buildOutputStream)

  def loadToIndexedSeq(buildInputStream: => InputStream): IndexedSeq[TripleItem] = loadToTraversable(buildInputStream).toIndexedSeq

  def loadToMap(buildInputStream: => InputStream): collection.Map[TripleItem, Int] = {
    val map = collection.mutable.HashMap.empty[TripleItem, Int]
    load(buildInputStream) { reader =>
      for ((tripleItem, i) <- Stream.continually(reader.read()).takeWhile(_.nonEmpty).map(_.get).zipWithIndex)
        map += (tripleItem -> i)
      map
    }
  }

}
