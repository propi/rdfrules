package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{PatternMatcher, Rule, RulePattern}
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class PredictedTriples private(val triples: ForEach[PredictedTriple], val index: Index)
  extends Transformable[PredictedTriple, PredictedTriples]
    with Cacheable[PredictedTriple, PredictedTriples]
    with Debugable[PredictedTriple, PredictedTriples] {

  self =>

  protected def coll: ForEach[PredictedTriple] = triples

  protected def transform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, index)

  protected def cachedTransform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, index)

  protected val serializer: Serializer[PredictedTriple] = implicitly[Serializer[PredictedTriple]]
  protected val deserializer: Deserializer[PredictedTriple] = implicitly[Deserializer[PredictedTriple]]
  protected val serializationSize: SerializationSize[PredictedTriple] = implicitly[SerializationSize[PredictedTriple]]
  protected val dataLoadingText: String = "Predicted triples loading"

  def withIndex(index: Index): PredictedTriples = new PredictedTriples(triples, index)

  def filter(pattern: RulePattern, patterns: RulePattern*): PredictedTriples = transform((f: PredictedTriple => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    implicit val thi: TripleIndex[Int] = index.tripleMap
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    triples.filter(triple => mappedPatterns.exists(rulePattern => triple.rules.exists(rule => rulePatternMatcher.matchPattern(rule, rulePattern)))).foreach(f)
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

  def distinct: PredictedTriples = transform(triples.distinct)

  def onlyFunctionalProperties: PredictedTriples = {
    val tm = index.tripleMap
    transform(triples.distinctBy { x =>
      val higherCardinalityConstant = tm.predicates(x.triple.predicate).higherCardinalitySide match {
        case TriplePosition.Subject => x.triple.subject
        case TriplePosition.Object => x.triple.`object`
      }
      higherCardinalityConstant -> x.triple.predicate
    })
  }

}

object PredictedTriples {

  def apply(index: Index, triples: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(triples, index)

}