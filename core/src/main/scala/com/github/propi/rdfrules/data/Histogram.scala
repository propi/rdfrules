package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.data.Histogram.Key
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
class Histogram private(col: ForEach[Key]) {
  private lazy val map = col.histogram

  def get(key: Key): Option[Int] = map.get(key)

  def iterator: Iterator[(Key, Int)] = map.iterator
}

object Histogram {

  case class Key(s: Option[TripleItem.Uri] = None, p: Option[TripleItem.Uri] = None, o: Option[TripleItem] = None) {
    def withSubject(uri: TripleItem.Uri): Key = this.copy(s = Some(uri))

    def withPredicate(uri: TripleItem.Uri): Key = this.copy(p = Some(uri))

    def withObject(tripleItem: TripleItem): Key = this.copy(o = Some(tripleItem))
  }

  def apply(col: ForEach[Key]): Histogram = new Histogram(col)

}
