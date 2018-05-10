package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
object Histogram {

  case class Key(s: Option[TripleItem.Uri], p: Option[TripleItem.Uri], o: Option[TripleItem]) {
    def withSubject(uri: TripleItem.Uri): Key = this.copy(s = Some(uri))

    def withPredicate(uri: TripleItem.Uri): Key = this.copy(p = Some(uri))

    def withObject(tripleItem: TripleItem): Key = this.copy(o = Some(tripleItem))
  }

  object Key {
    def apply(): Key = new Key(None, None, None)
  }

  def apply(col: Traversable[Key]): collection.Map[Key, Int] = new collection.Map[Key, Int] {
    private lazy val map = col.histogram

    def get(key: Key): Option[Int] = map.get(key)

    def iterator: Iterator[(Key, Int)] = map.iterator

    def +[V1 >: Int](kv: (Key, V1)): collection.Map[Key, V1] = map + kv

    def -(key: Key): collection.Map[Key, Int] = map - key
  }

}
