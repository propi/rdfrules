package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.data.{Histogram, PredicateInfo, Triple, TripleItem, TripleItemType}

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait TriplesOps {

  def triples: TripleTraversableView

  def histogram(subject: Boolean = false, predicate: Boolean = false, `object`: Boolean = false): collection.Map[Histogram.Key, Int] = {
    def boolToOpt[T](x: T, bool: Boolean) = if (bool) Some(x) else None

    def tripleToKey(triple: Triple) = Histogram.Key(
      boolToOpt(triple.subject, subject),
      boolToOpt(triple.predicate, predicate),
      boolToOpt(triple.`object`, `object`)
    )

    Histogram(triples.map(tripleToKey))
  }

  def types(): collection.Map[TripleItem.Uri, collection.Map[TripleItemType, Int]] = PredicateInfo(triples)

}
