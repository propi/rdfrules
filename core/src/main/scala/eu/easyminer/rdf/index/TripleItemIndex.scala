package eu.easyminer.rdf.index

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.data.{Dataset, TripleItem}

import eu.easyminer.rdf.data.TripleItemSerialization._

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleItemIndex extends Index[TripleItem] {

  def save(dataset: Dataset, buildOutputStream: => OutputStream): Unit = save(dataset.toTriples.flatMap(triple => List(triple.subject, triple.predicate, triple.`object`)).toSet, buildOutputStream)

  def loadToIndexedSeq(buildInputStream: => InputStream): IndexedSeq[TripleItem] = loadToTraversable(buildInputStream).toIndexedSeq

  def loadToMap(buildInputStream: => InputStream): collection.Map[TripleItem, Int] = {
    val map = collection.mutable.HashMap.empty[TripleItem, Int]
    load(buildInputStream) { reader =>
      for ((tripleItem, i) <- Stream.continually(reader.read()).takeWhile(_.nonEmpty).map(_.get).zipWithIndex)
        map += (tripleItem -> (i + 1))
      map
    }
  }

}
