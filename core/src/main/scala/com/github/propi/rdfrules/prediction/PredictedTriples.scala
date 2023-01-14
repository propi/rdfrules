package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.Graph
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{AutoIndex, IndexCollections, TrainTestIndex, TripleItemIndex}
import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.{RulesFactory, ScoreFactory}
import com.github.propi.rdfrules.prediction.eval.{EvaluationBuilder, EvaluationResult}
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{PatternMatcher, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.TripleSerialization._
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import java.io._
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class PredictedTriples private(val triples: ForEach[PredictedTriple], val parallelism: Int)(implicit val index: TrainTestIndex)
  extends Transformable[PredictedTriple, PredictedTriples]
    with Cacheable[PredictedTriple, PredictedTriples]
    with Sortable[PredictedTriple, PredictedTriples]
    with Debugable[PredictedTriple, PredictedTriples] {

  self =>

  implicit private def mapper: TripleItemIndex = index.test.tripleItemMap

  protected def serializer: Serializer[PredictedTriple] = Serializer.by[PredictedTriple, ResolvedPredictedTriple](ResolvedPredictedTriple(_))

  protected def deserializer: Deserializer[PredictedTriple] = Deserializer.by[ResolvedPredictedTriple, PredictedTriple](_.toPredictedTriple)

  protected def ordering: Ordering[PredictedTriple] = Ordering.by(x => -x.score -> x.rules.head)

  protected def serializationSize: SerializationSize[PredictedTriple] = SerializationSize.by[PredictedTriple, ResolvedPredictedTriple]

  protected def dataLoadingText: String = "Predicted triples loading"

  protected def coll: ForEach[PredictedTriple] = triples

  protected def transform(col: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(col, parallelism)

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

  def grouped(scoreFactory: ScoreFactory, rulesFactory: RulesFactory, limit: Int = -1): PredictedTriples = {
    transform(triples.groupedBy(limit)(_.triple)(PredictedTriplesAggregator(scoreFactory, rulesFactory)).map(_._2))
  }

  class PredictionTasksResults private[PredictedTriples](coll: ForEach[PredictionTaskResult]) extends ForEach[(PredictionTask.Resolved, PredictedTriples)] {
    def foreach(f: ((PredictionTask.Resolved, PredictedTriples)) => Unit): Unit = coll.map(x => PredictionTask.Resolved(x.predictionTask) -> transform(x.predictedTriples)).foreach(f)

    def evaluate(evaluator: EvaluationBuilder, evaluators: EvaluationBuilder*): List[EvaluationResult] = {
      for (predictionTaskResult <- coll) {
        evaluator.evaluate(predictionTaskResult)(index.test.tripleMap)
        evaluators.foreach(_.evaluate(predictionTaskResult)(index.test.tripleMap))
      }
      List.from(Iterator(evaluator.build) ++ evaluators.iterator.map(_.build))
    }

    def onlyFunctionalPredictions: PredictedTriples = transform(coll.flatMap(_.headOption))

    def onlyQpcaPredictions: PredictedTriples = transform(coll.flatMap(_.filterByQpca(index.train.tripleMap).predictedTriples))
  }

  def predictionTasks(predictionTasksBuilder: PredictionTasksBuilder = PredictionTasksBuilder.FromTestSet.FromPredicateCardinalities, limit: Int = -1, topK: Int = -1): PredictionTasksResults = {
    val coll: ForEach[PredictionTaskResult] = predictionTasksBuilder match {
      case builder: PredictionTasksBuilder.FromPredictedTriple => triples.flatMap(x => builder.build(x).map(_ -> x)).groupedBy(limit)(_._1)(PredictionTaskResult.factory(topK)).map(_._2)
      case builder: PredictionTasksBuilder.FromData => (f: PredictionTaskResult => Unit) => {
        val hset = collection.mutable.Set.empty[PredictionTask]
        builder.build.take(limit).foreach(hset.addOne)
        triples.flatMap { predictedTriple =>
          val (taskHead, taskTail) = predictedTriple.predictionTasks
          ForEach(taskHead -> predictedTriple, taskTail -> predictedTriple)
        }.filter(x => hset(x._1)).groupedBy()(_._1)(PredictionTaskResult.factory(topK)).foreach { case (predictionTask, result) =>
          hset.remove(predictionTask)
          f(result)
        }
        hset.foreach(predictionTask => f(PredictionTaskResult.empty(predictionTask)))
      }
    }
    new PredictionTasksResults(coll)
  }

  def withoutTrainTriples: PredictedTriples = filter(x => !index.train.tripleMap.contains(x.triple))

  def withCoveredTestPredictionTasks: PredictedTriples = {
    val testIndex = index.test.tripleMap

    def isInTest(p: Int, x: Int): Boolean = testIndex.predicates.get(p).exists(pi => pi.subjects.contains(x) || pi.objects.contains(x))

    filter(x => isInTest(x.triple.p, x.triple.s) || isInTest(x.triple.p, x.triple.o))
  }

  def onlyPcaPredictions: PredictedTriples = filter(_.predictedResult == PredictedResult.PcaPositive)

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
    new PredictedTriples(triples, normParallelism)
  }
}

object PredictedTriples {
  private def resolvedReader(file: File)(implicit reader: PredictionReader): PredictionReader = if (reader == PredictionReader.NoReader) PredictionReader(file) else reader

  def apply(index: TrainTestIndex, triples: ForEach[PredictedTriple]): PredictedTriples = new PredictedTriples(triples, Runtime.getRuntime.availableProcessors())(index)

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