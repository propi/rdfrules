package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.data.{Histogram, PredicateInfo, Triple}

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait TriplesOps {

  def triples: TripleTraversableView

  def histogram(s: Boolean, p: Boolean, o: Boolean): Histogram = {
    def boolToOpt[T](x: T, bool: Boolean) = if (bool) Some(x) else None

    def tripleToKey(triple: Triple) = Histogram.Key(
      boolToOpt(triple.subject, s),
      boolToOpt(triple.predicate, p),
      boolToOpt(triple.`object`, o)
    )

    Histogram(triples.map(tripleToKey))
  }

  def types(): Traversable[PredicateInfo] = PredicateInfo(triples)

}
