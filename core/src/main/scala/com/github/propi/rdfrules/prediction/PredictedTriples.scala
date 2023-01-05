package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.Graph
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{AutoIndex, Index, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{Atom, PatternMatcher, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}
import com.github.propi.rdfrules.utils.{ForEach, IncrementalInt}

import java.io._
import scala.language.implicitConversions

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

  protected def cachedTransform(col: ForEach[PredictedTriple]): PredictedTriples = transform(col)

  protected implicit val ordering: Ordering[PredictedTriple] = Ordering.by(_.rules.head)
  protected val serializer: Serializer[PredictedTriple] = Serializer.by[PredictedTriple, ResolvedPredictedTriple](ResolvedPredictedTriple(_)(index.tripleItemMap))
  protected val deserializer: Deserializer[PredictedTriple] = Deserializer.by[ResolvedPredictedTriple, PredictedTriple](_.toPredictedTriple(index.tripleItemMap))
  protected val serializationSize: SerializationSize[PredictedTriple] = SerializationSize.by[PredictedTriple, ResolvedPredictedTriple]
  protected val dataLoadingText: String = "Predicted triples loading"

  def singleTriples: ForEach[PredictedTriple.Single] = coll.flatMap(_.toSinglePredictedTriples)

  def withIndex(index: Index): PredictedTriples = new PredictedTriples(triples, index)

  def filter(pattern: RulePattern, patterns: RulePattern*): PredictedTriples = transform((f: PredictedTriple => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    implicit val thi: TripleIndex.Builder[Int] = index
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    triples.filter(triple => mappedPatterns.exists(rulePattern => triple.rules.exists(rule => rulePatternMatcher.matchPattern(rule, rulePattern)(Aliases.empty).isDefined))).foreach(f)
  })

  implicit private def resolvePredictedTriple(predictedTriple: PredictedTriple): ResolvedPredictedTriple = ResolvedPredictedTriple(predictedTriple)(index.tripleItemMap)

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

  def grouped(limit: Int = -1): PredictedTriples = transform(triples.groupedBy(limit)(_.triple).map(_.reduce(_ :++ _.rules)))

  def onlyPcaPredictions: PredictedTriples = filter(_.predictedResult == PredictedResult.PcaPositive).onlyFunctionalPredictions

  def onlyQpcaPredictions: PredictedTriples = {
    implicit val tm: TripleIndex[Int] = index.tripleMap
    transform((f: PredictedTriple => Unit) => {
      val hmap = collection.mutable.Map.empty[PredictionTask, IncrementalInt]
      for (triple <- triples) {
        val predictionTask = PredictionTask(triple)
        val currentCardinality = hmap.getOrElseUpdate(predictionTask, IncrementalInt(predictionTask.index.size))
        val maxCardinalityThreshold = tm.predicates.get(triple.triple.p).map(_.averageCardinality).getOrElse(1)
        if (currentCardinality.getValue < maxCardinalityThreshold && !tm.contains(triple.triple)) {
          f(triple)
          currentCardinality.++
        }
      }
    })
  }

  def onlyFunctionalPredictions: PredictedTriples = transform(triples.distinctBy(PredictionTask(_)(index.tripleMap)))

  def evaluate(pca: Boolean, injectiveMapping: Boolean = true): EvaluationResult = {
    var tp, fp = 0
    val predictedProperties = collection.mutable.HashMap.empty[Int, (Int, IncrementalInt)]
    val predictedTriples = collection.mutable.HashSet.empty[IntTriple]
    for (triple <- singleTriples) {
      val max = index.tripleMap.predicates.get(triple.triple.p).map { p =>
        (triple.rule.head.subject, triple.rule.head.`object`) match {
          case (_: Atom.Variable, _: Atom.Variable) => p.size(injectiveMapping)
          case (Atom.Constant(x), _: Atom.Variable) => p.subjects.get(x).map(_.size(injectiveMapping)).getOrElse(0)
          case (_: Atom.Variable, Atom.Constant(x)) => p.objects.get(x).map(_.size(injectiveMapping)).getOrElse(0)
          case (Atom.Constant(s), Atom.Constant(o)) => if (injectiveMapping && s == o || !p.subjects.get(s).exists(_.contains(o))) 0 else 1
        }
      }.getOrElse(0)
      val (pmax, pcounter) = predictedProperties.getOrElseUpdate(
        triple.triple.p,
        max -> IncrementalInt()
      )
      if (pmax < max) predictedProperties.update(triple.triple.p, max -> pcounter)
      if (!predictedTriples(triple.triple)) {
        predictedTriples += triple.triple
        triple.predictedResult match {
          case PredictedResult.Positive =>
            tp += 1
            pcounter += 1
          case PredictedResult.Negative =>
            fp += 1
          case PredictedResult.PcaPositive =>
            if (!pca) fp += 1
        }
      }
    }
    EvaluationResult(tp, fp, math.max(0, predictedProperties.valuesIterator.map(x => x._1 - x._2.getValue).sum), 0)
  }

  def toGraph: Graph = Graph(distinctPredictions.resolvedTriples.map(_.triple))

  def `export`(os: => OutputStream)(implicit writer: PredictionWriter): Unit = writer.writeToOutputStream(this, os)

  def `export`(file: File)(implicit writer: PredictionWriter): Unit = {
    val newWriter = if (writer == PredictionWriter.NoWriter) PredictionWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: PredictionWriter): Unit = `export`(new File(file))
}

object PredictedTriples {
  private def resolvedReader(file: File)(implicit reader: PredictionReader): PredictionReader = if (reader == PredictionReader.NoReader) PredictionReader(file) else reader

  def apply(index: Index, triples: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(triples, index)

  def apply(index: Index, triples: ForEach[ResolvedPredictedTriple])(implicit i1: DummyImplicit): PredictedTriples = apply(index, triples.map(_.toPredictedTriple(index.tripleItemMap)))

  def apply(triples: ForEach[ResolvedPredictedTriple]): PredictedTriples = apply(AutoIndex(), triples)

  def apply(index: Index, file: File)(implicit reader: PredictionReader): PredictedTriples = apply(index, resolvedReader(file).fromFile(file))

  def apply(index: Index, file: String)(implicit reader: PredictionReader): PredictedTriples = apply(index, new File(file))

  def apply(index: Index, is: => InputStream)(implicit reader: PredictionReader): PredictedTriples = apply(index, reader.fromInputStream(is))

  def apply(file: File)(implicit reader: PredictionReader): PredictedTriples = apply(resolvedReader(file).fromFile(file))

  def apply(file: String)(implicit reader: PredictionReader): PredictedTriples = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: PredictionReader): PredictedTriples = apply(reader.fromInputStream(is))

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