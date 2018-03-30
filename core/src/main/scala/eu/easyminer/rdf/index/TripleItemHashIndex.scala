package eu.easyminer.rdf.index

import eu.easyminer.rdf.data.TripleItem

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class TripleItemHashIndex private(map: collection.Map[Int, TripleItem]) {

  def getIndex(x: TripleItem): Int = Stream.iterate(x.hashCode())(_ + 1).find(i => map(i) == x).get

  def getTripleItem(x: Int): TripleItem = map(x)

  def iterator: Iterator[(Int, TripleItem)] = map.iterator

}

object TripleItemHashIndex {

  def fromIndexedItem(col: Traversable[(Int, TripleItem)]): TripleItemHashIndex = {
    new TripleItemHashIndex(collection.mutable.HashMap.empty[Int, TripleItem] ++= col)
  }

  def apply(col: Traversable[TripleItem]): TripleItemHashIndex = {
    val map = collection.mutable.HashMap.empty[Int, TripleItem]
    for (item <- col) {
      Stream.iterate(item.hashCode())(_ + 1).map(i => i -> map.get(i)).find(_._2.forall(_ == item)).get match {
        case (i, None) => map += (i -> item)
        case _ =>
      }
    }
    new TripleItemHashIndex(map)
  }

}
