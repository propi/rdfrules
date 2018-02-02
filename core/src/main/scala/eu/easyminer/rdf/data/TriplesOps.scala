package eu.easyminer.rdf.data

import eu.easyminer.rdf.data.Triple.TripleTraversableView

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait TriplesOps {

  def toTriples: TripleTraversableView

  def histogram(s: Boolean, p: Boolean, o: Boolean): Histogram = {
    def boolToOpt[T](x: T, bool: Boolean) = if (s) Some(x) else None

    def tripleToKey(triple: Triple) = Histogram.Key(
      boolToOpt(triple.subject, s),
      boolToOpt(triple.predicate, p),
      boolToOpt(triple.`object`, o)
    )

    Histogram(toTriples.map(tripleToKey))
  }

  def types(): Vector[PredicateInfo] = PredicateInfo(toTriples)

}
