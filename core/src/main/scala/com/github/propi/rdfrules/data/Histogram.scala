package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.data.Histogram.Key
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
class Histogram private(col: Traversable[Key]) extends Traversable[(Key, Int)] {
  private lazy val map = col.histogram

  def get(key: Key): Option[Int] = map.get(key)

  def foreach[U](f: ((Key, Int)) => U): Unit = map.foreach(f)

  def getMap: collection.Map[Key, Int] = map

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
