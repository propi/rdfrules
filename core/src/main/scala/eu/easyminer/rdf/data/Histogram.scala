package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.Histogram.Key
import eu.easyminer.rdf.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
class Histogram private(col: Traversable[Key]) extends Traversable[(Key, Int)] {
  private lazy val map = col.histogram

  def get(key: Key): Option[Int] = map.get(key)

  def foreach[U](f: ((Key, Int)) => U): Unit = map.foreach(f)

  override def toMap[T, U](implicit ev: <:<[(Key, Int), (T, U)]): Map[T, U] = map.toMap
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

  def apply(col: Traversable[Key]): Histogram = new Histogram(col)

}
