package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.Histogram.Key
import eu.easyminer.rdf.utils.IncrementalInt

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
class Histogram private(map: collection.Map[Key, Int]) extends Iterable[(Key, Int)] {
  def iterator: Iterator[(Key, Int)] = map.iterator

  def get(key: Key): Option[Int] = map.get(key)
}

object Histogram {

  case class Key(s: Option[TripleItem.Uri], p: Option[TripleItem.Uri], o: Option[TripleItem]) {
    def withSubject(uri: TripleItem.Uri): Key = this.copy(s = Some(uri))

    def withPredicate(uri: TripleItem.Uri): Key = this.copy(p = Some(uri))

    def withObject(tripleItem: TripleItem): Key = this.copy(o = Some(tripleItem))
  }

  object Key {
    def apply(): Key = new Key(None, None, None)
  }

  def apply(col: TraversableOnce[Key]): Histogram = {
    val map = collection.mutable.HashMap.empty[Key, IncrementalInt]
    col.foreach(map.getOrElseUpdate(_, IncrementalInt()).++)
    new Histogram(map.mapValues(_.getValue))
  }

}
