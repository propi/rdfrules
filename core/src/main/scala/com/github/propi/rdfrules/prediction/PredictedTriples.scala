package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.data.{Graph, TriplePosition}
import com.github.propi.rdfrules.index.IndexItem.IntTriple
import com.github.propi.rdfrules.index.{AutoIndex, IndexCollections, TrainTestIndex, TripleItemIndex}
import com.github.propi.rdfrules.prediction.RankingEvaluationResult.Hits
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
class PredictedTriples private(val triples: ForEach[PredictedTriple], val index: TrainTestIndex, val parallelism: Int)
  extends Transformable[PredictedTriple, PredictedTriples]
    with Cacheable[PredictedTriple, PredictedTriples]
    with Sortable[PredictedTriple, PredictedTriples]
    with Debugable[PredictedTriple, PredictedTriples] {

  self =>

  implicit private def mapper: TripleItemIndex = index.test.tripleItemMap

  protected def serializer: Serializer[PredictedTriple] = Serializer.by[PredictedTriple, ResolvedPredictedTriple](ResolvedPredictedTriple(_))

  protected def deserializer: Deserializer[PredictedTriple] = Deserializer.by[ResolvedPredictedTriple, PredictedTriple](_.toPredictedTriple)

  protected def ordering: Ordering[PredictedTriple] = Ordering.by(x => (x match {
    case x: PredictedTriple.Scored => -x.score
    case _ => 0.0
  }) -> x.rules.head)

  protected def serializationSize: SerializationSize[PredictedTriple] = SerializationSize.by[PredictedTriple, ResolvedPredictedTriple]

  protected def dataLoadingText: String = "Predicted triples loading"

  protected def coll: ForEach[PredictedTriple] = triples

  protected def transform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, index, parallelism)

  protected def cachedTransform(col: ForEach[PredictedTriple]): PredictedTriples = transform(col)

  def singleTriples: ForEach[PredictedTriple.Single] = coll.flatMap(_.toSinglePredictedTriples)

  def filter(pattern: RulePattern, patterns: RulePattern*): PredictedTriples = transform((f: PredictedTriple => Unit) => {
    implicit val thi: IndexCollections.Builder[Int] = index.train
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    triples.filter(triple => mappedPatterns.exists(rulePattern => triple.rules.exists(rule => rulePatternMatcher.matchPattern(rule, rulePattern)(Aliases.empty).isDefined))).foreach(f)
  })

  implicit private def resolvePredictedTriple(predictedTriple: PredictedTriple): ResolvedPredictedTriple = ResolvedPredictedTriple(predictedTriple)

  def filterResolved(f: ResolvedPredictedTriple => Boolean): PredictedTriples = transform((f2: PredictedTriple => Unit) => {
    triples.filter(x => f(x)).foreach(f2)
  })

  def resolvedTriples: ForEach[ResolvedPredictedTriple] = new ForEach[ResolvedPredictedTriple] {
    def foreach(f: ResolvedPredictedTriple => Unit): Unit = {
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

  private def predictionTaskBuilder(targetVariable: ConceptPosition): PredictedTriple => ForEach[PredictionTask] = x => ForEach(x.predictionTask(targetVariable))

  private def predictionTaskBuilder(predictionTaskPatterns: Set[PredictionTaskPattern]): PredictedTriple => ForEach[PredictionTask] = triple => {
    val (pattern1, pattern2) = triple.predictionTaskPatterns
    ForEach(pattern1, pattern2).filter(predictionTaskPatterns).map(x => triple.predictionTask(x.targetVariable))
  }

  private def predictionTaskBuilder: PredictedTriple => ForEach[PredictionTask] = x => ForEach(x.predictionTask(index.train.tripleMap))

  private def predictionTasks(limit: Int, buildPredictionTask: PredictedTriple => ForEach[PredictionTask]): ForEach[PredictedTriples] = {
    triples.flatMap(x => buildPredictionTask(x).map(_ -> x)).groupedBy(limit)(_._1).map(triples => new PredictedTriples(triples.map(_._2), index, parallelism))
  }

  def predictionTasks(limit: Int, predictionTaskPatterns: Set[PredictionTaskPattern]): ForEach[PredictedTriples] = predictionTasks(limit, predictionTaskBuilder(predictionTaskPatterns))

  def predictionTasks(predictionTaskPatterns: Set[PredictionTaskPattern]): ForEach[PredictedTriples] = predictionTasks(-1, predictionTaskPatterns)

  def predictionTasks(limit: Int, targetVariable: ConceptPosition): ForEach[PredictedTriples] = predictionTasks(limit, predictionTaskBuilder(targetVariable))

  def predictionTasks(targetVariable: ConceptPosition): ForEach[PredictedTriples] = predictionTasks(-1, targetVariable)

  def predictionTasks(limit: Int): ForEach[PredictedTriples] = predictionTasks(limit, predictionTaskBuilder)

  def predictionTasks: ForEach[PredictedTriples] = predictionTasks(-1)

  def scoreGroups(implicit predictionScorer: PredictionScorer): PredictedTriples = transform(triples.parMap(parallelism)(x => x.withScore(predictionScorer.score(x))))

  def onlyNewTriples: PredictedTriples = filter(x => !index.train.tripleMap.contains(x.triple))

  def onlyPcaPredictions: PredictedTriples = filter(_.predictedResult == PredictedResult.PcaPositive)

  private def onlyQpcaPredictions(buildPredictionTask: PredictedTriple => ForEach[PredictionTask]): PredictedTriples = {
    transform((f: PredictedTriple => Unit) => {
      val hmap = collection.mutable.Map.empty[PredictionTask, IncrementalInt]
      for {
        triple <- triples
        predictionTask <- buildPredictionTask(triple)
      } {
        val currentCardinality = hmap.getOrElseUpdate(predictionTask, IncrementalInt(predictionTask.index(index.train.tripleMap).size))
        val maxCardinalityThreshold = index.train.tripleMap.predicates.get(triple.triple.p).map(x => if (predictionTask.predictionTaskPattern.targetVariable == TriplePosition.Subject) x.averageObjectCardinality else x.averageSubjectCardinality).getOrElse(1)
        if (currentCardinality.getValue < maxCardinalityThreshold && !index.train.tripleMap.contains(triple.triple)) {
          f(triple)
          currentCardinality.++
        }
      }
    })
  }

  def onlyQpcaPredictions(targetVariable: ConceptPosition): PredictedTriples = onlyQpcaPredictions(predictionTaskBuilder(targetVariable))

  def onlyQpcaPredictions(predictionTaskPatterns: Set[PredictionTaskPattern]): PredictedTriples = onlyQpcaPredictions(predictionTaskBuilder(predictionTaskPatterns))

  def onlyQpcaPredictions: PredictedTriples = onlyQpcaPredictions(predictionTaskBuilder)

  private def onlyFunctionalPredictions(buildPredictionTask: PredictedTriple => ForEach[PredictionTask]): PredictedTriples = transform(triples.flatMap(triple => buildPredictionTask(triple).map(_ -> triple)).distinctBy(_._1).map(_._2))

  def onlyFunctionalPredictions: PredictedTriples = onlyFunctionalPredictions(predictionTaskBuilder)

  def onlyFunctionalPredictions(predictionTaskPatterns: Set[PredictionTaskPattern]): PredictedTriples = onlyFunctionalPredictions(predictionTaskBuilder(predictionTaskPatterns))

  def onlyFunctionalPredictions(targetVariable: ConceptPosition): PredictedTriples = onlyFunctionalPredictions(predictionTaskBuilder(targetVariable))

  private def evaluateRanking(hits: IndexedSeq[Int], buildPredictionTask: PredictedTriple => ForEach[PredictionTask]): RankingEvaluationResult = {
    var q = 0
    var qr = 0
    val sumHits = Array.fill(hits.length)(0)
    var sumRank = 0
    var sumiRank = 0.0
    for (predictedTriples <- predictionTasks(-1, buildPredictionTask)) {
      val rank = predictedTriples.triples.zipWithIndex.find(x => index.test.tripleMap.contains(x._1.triple)).map(_._2 + 1).getOrElse(Int.MaxValue)
      val isCorrect = rank != Int.MaxValue
      q += 1
      for (i <- hits.indices if rank <= hits(i)) {
        sumHits(i) += 1
      }
      if (isCorrect) {
        qr += 1
        sumRank += rank
        sumiRank += 1.0 / rank
      }
    }
    val qDouble = q.toDouble
    val qrDouble = q.toDouble
    RankingEvaluationResult(
      if (q == 0) hits.map(k => Hits(k, 0.0)) else hits.iterator.zipWithIndex.map { case (k, i) => Hits(k, sumHits(i) / qDouble) }.toSeq,
      if (qr == 0) 0.0 else sumRank / qrDouble,
      if (qr == 0) 0.0 else sumiRank / qrDouble,
      q,
      qr
    )
  }

  def evaluateRanking(hits: IndexedSeq[Int]): RankingEvaluationResult = evaluateRanking(hits, predictionTaskBuilder)

  def evaluateRanking(hits: IndexedSeq[Int], predictionTaskPatterns: Set[PredictionTaskPattern]): RankingEvaluationResult = evaluateRanking(hits, predictionTaskBuilder(predictionTaskPatterns))

  def evaluateRanking(hits: IndexedSeq[Int], targetVariable: ConceptPosition): RankingEvaluationResult = evaluateRanking(hits, predictionTaskBuilder(targetVariable))

  def evaluateCompleteness(pca: Boolean, injectiveMapping: Boolean = true): CompletenessEvaluationResult = {
    var tp, fp = 0
    val predictedProperties = collection.mutable.HashMap.empty[Int, (Int, IncrementalInt)]
    val predictedTriples = collection.mutable.HashSet.empty[IntTriple]
    for (triple <- singleTriples) {
      val max = index.test.tripleMap.predicates.get(triple.triple.p).map { p =>
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
    CompletenessEvaluationResult(tp, fp, math.max(0, predictedProperties.valuesIterator.map(x => x._1 - x._2.getValue).sum), 0)
  }

  def toGraph: Graph = Graph(distinctPredictions.resolvedTriples.map(_.triple))

  def `export`(os: => OutputStream)(implicit writer: PredictionWriter): Unit = writer.writeToOutputStream(this, os)

  def `export`(file: File)(implicit writer: PredictionWriter): Unit = {
    val newWriter = if (writer == PredictionWriter.NoWriter) PredictionWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: PredictionWriter): Unit = `export`(new File(file))

  def setParallelism(parallelism: Int): PredictedTriples = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    new PredictedTriples(triples, index, normParallelism)
  }
}

object PredictedTriples {
  private def resolvedReader(file: File)(implicit reader: PredictionReader): PredictionReader = if (reader == PredictionReader.NoReader) PredictionReader(file) else reader

  def apply(index: TrainTestIndex, triples: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(triples, index, Runtime.getRuntime.availableProcessors())

  def apply(index: TrainTestIndex, triples: ForEach[ResolvedPredictedTriple])(implicit i1: DummyImplicit): PredictedTriples = apply(index, triples.map(_.toPredictedTriple(index.test.tripleItemMap)))

  def apply(triples: ForEach[ResolvedPredictedTriple]): PredictedTriples = apply(TrainTestIndex(AutoIndex()), triples)

  def apply(index: TrainTestIndex, file: File)(implicit reader: PredictionReader): PredictedTriples = apply(index, resolvedReader(file).fromFile(file))

  def apply(index: TrainTestIndex, file: String)(implicit reader: PredictionReader): PredictedTriples = apply(index, new File(file))

  def apply(index: TrainTestIndex, is: => InputStream)(implicit reader: PredictionReader): PredictedTriples = apply(index, reader.fromInputStream(is))

  def apply(file: File)(implicit reader: PredictionReader): PredictedTriples = apply(resolvedReader(file).fromFile(file))

  def apply(file: String)(implicit reader: PredictionReader): PredictedTriples = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: PredictionReader): PredictedTriples = apply(reader.fromInputStream(is))

  def fromCache(index: TrainTestIndex, is: => InputStream): PredictedTriples = apply(
    index,
    (f: PredictedTriple => Unit) => Deserializer.deserializeFromInputStream[ResolvedPredictedTriple, Unit](is) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).map(_.toPredictedTriple(index.test.tripleItemMap)).foreach(f)
    }
  )

  def fromCache(index: TrainTestIndex, file: File): PredictedTriples = fromCache(index, new FileInputStream(file))

  def fromCache(index: TrainTestIndex, file: String): PredictedTriples = fromCache(index, new File(file))

  def fromCache(is: => InputStream): PredictedTriples = fromCache(TrainTestIndex(AutoIndex()), is)

  def fromCache(file: File): PredictedTriples = fromCache(new FileInputStream(file))

  def fromCache(file: String): PredictedTriples = fromCache(new File(file))
}