package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{AutoIndex, Index, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{PatternMatcher, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import java.io.{File, FileInputStream, InputStream}

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class PredictedTriples private(val triples: ForEach[PredictedTriple], val index: Index)
  extends Transformable[PredictedTriple, PredictedTriples]
    with Cacheable[PredictedTriple, PredictedTriples]
    with Sortable[PredictedTriple, PredictedTriples]
    with Debugable[PredictedTriple, PredictedTriples] {

  self =>

  protected def coll: ForEach[PredictedTriple] = triples

  protected def transform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, index)

  protected def cachedTransform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, index)

  protected implicit val ordering: Ordering[PredictedTriple] = Ordering.by(_.rule)
  protected val serializer: Serializer[PredictedTriple] = Serializer.by[PredictedTriple, ResolvedPredictedTriple](ResolvedPredictedTriple(_)(index.tripleItemMap))
  protected val deserializer: Deserializer[PredictedTriple] = Deserializer.by[ResolvedPredictedTriple, PredictedTriple](_.toPredictedTriple(index.tripleItemMap))
  protected val serializationSize: SerializationSize[PredictedTriple] = SerializationSize.by[PredictedTriple, ResolvedPredictedTriple]
  protected val dataLoadingText: String = "Predicted triples loading"

  def withIndex(index: Index): PredictedTriples = new PredictedTriples(triples, index)

  def filter(pattern: RulePattern, patterns: RulePattern*): PredictedTriples = transform((f: PredictedTriple => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    implicit val thi: TripleIndex.Builder[Int] = index
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    triples.filter(triple => mappedPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(triple.rule, rulePattern))).foreach(f)
  })

  def filterResolved(f: ResolvedPredictedTriple => Boolean): PredictedTriples = transform((f2: PredictedTriple => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    triples.filter(x => f(x)).foreach(f2)
  })

  def resolvedTriples: ForEach[ResolvedPredictedTriple] = new ForEach[ResolvedPredictedTriple] {
    def foreach(f: ResolvedPredictedTriple => Unit): Unit = {
      implicit val mapper: TripleItemIndex = index.tripleItemMap
      triples.foreach(x => f(x))
    }

    override def knownSize: Int = triples.knownSize
  }

  def foreach(f: ResolvedPredictedTriple => Unit): Unit = resolvedTriples.foreach(f)

  def +(predictedTriples: PredictedTriples): PredictedTriples = transform(triples.concat(predictedTriples.triples))

  def headResolved: ResolvedPredictedTriple = resolvedTriples.head

  def headResolvedOption: Option[ResolvedPredictedTriple] = resolvedTriples.headOption

  def findResolved(f: ResolvedPredictedTriple => Boolean): Option[ResolvedPredictedTriple] = resolvedTriples.find(f)

  def distinctPredictions: PredictedTriples = transform(triples.distinctBy(_.triple))

  def onlyFunctionalPredictions: PredictedTriples = {
    val tm = index.tripleMap
    transform(triples.distinctBy { x =>
      val higherCardinalityConstant = tm.predicates(x.triple.p).higherCardinalitySide match {
        case TriplePosition.Subject => x.triple.s
        case TriplePosition.Object => x.triple.o
      }
      higherCardinalityConstant -> x.triple.p
    })
  }

}

object PredictedTriples {
  def apply(index: Index, triples: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(triples, index)

  def apply(index: Index, triples: ForEach[ResolvedPredictedTriple])(implicit i1: DummyImplicit): PredictedTriples = apply(index, triples.map(_.toPredictedTriple(index.tripleItemMap)))

  def apply(triples: ForEach[ResolvedPredictedTriple]): PredictedTriples = apply(AutoIndex(), triples)

  def fromCache(index: Index, is: => InputStream): PredictedTriples = apply(
    index,
    (f: PredictedTriple => Unit) => Deserializer.deserializeFromInputStream[ResolvedPredictedTriple, Unit](is) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).map(_.toPredictedTriple(index.tripleItemMap)).foreach(f)
    }
  )

  def fromCache(index: Index, file: File): PredictedTriples = fromCache(index, new FileInputStream(file))

  def fromCache(index: Index, file: String): PredictedTriples = fromCache(index, new File(file))

  def fromCache(is: => InputStream): PredictedTriples = fromCache(AutoIndex(), is)

  def fromCache(file: File): PredictedTriples = fromCache(new FileInputStream(file))

  def fromCache(file: String): PredictedTriples = fromCache(new File(file))
}