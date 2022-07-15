package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleItemIndex}
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{Measure, PatternMatcher, ResolvedRule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetReader, RulesetWriter}
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.{Debugger, ForEach}
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import java.io._
import scala.math.Ordering.Implicits.seqOrdering

/**
  * Created by Vaclav Zeman on 14. 10. 2019.
  */
class Model private(val rules: ForEach[ResolvedRule], val parallelism: Int)
  extends Transformable[ResolvedRule, Model]
    with Cacheable[ResolvedRule, Model]
    with Sortable[ResolvedRule, Model]
    with Debugable[ResolvedRule, Model] {

  protected val serializer: Serializer[ResolvedRule] = implicitly[Serializer[ResolvedRule]]
  protected val deserializer: Deserializer[ResolvedRule] = implicitly[Deserializer[ResolvedRule]]
  protected val serializationSize: SerializationSize[ResolvedRule] = implicitly[SerializationSize[ResolvedRule]]
  protected val ordering: Ordering[ResolvedRule] = implicitly[Ordering[ResolvedRule]]
  protected val dataLoadingText: String = "Model loading"

  protected def cachedTransform(col: ForEach[ResolvedRule]): Model = new Model(col, parallelism)

  protected def coll: ForEach[ResolvedRule] = rules

  protected def transform(col: ForEach[ResolvedRule]): Model = new Model(col, parallelism)

  /**
    * TODO in parallel prediction
    * Set number of workers for prediction in parallel
    * The parallelism should be equal to or lower than the max thread pool size of the execution context
    *
    * @param parallelism number of workers
    * @return
    */
  def setParallelism(parallelism: Int): Model = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    new Model(rules, normParallelism)
  }

  def filter(pattern: RulePattern, patterns: RulePattern*): Model = {
    val allPatterns = pattern +: patterns
    val rulePatternMatcher = implicitly[PatternMatcher[ResolvedRule, RulePattern]]
    filter(rule => allPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule, rulePattern)))
  }

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Model = sortBy { rule =>
    rule.measures(measure) +: measures.map(rule.measures(_))
  }

  def sortByRuleLength(measures: Key[Measure]*): Model = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)))
  }

  def foreach(f: ResolvedRule => Unit): Unit = rules.foreach(f)

  def `export`(os: => OutputStream)(implicit writer: RulesetWriter): Unit = writer.writeToOutputStream(rules, os)

  def `export`(file: File)(implicit writer: RulesetWriter): Unit = {
    val newWriter = if (writer == RulesetWriter.NoWriter) RulesetWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: RulesetWriter): Unit = export(new File(file))

  def toRuleset(graph: Graph): Ruleset = toRuleset(graph.index)

  def toRuleset(graph: Graph)(implicit debugger: Debugger): Ruleset = toRuleset(graph.index)

  def toRuleset(dataset: Dataset): Ruleset = toRuleset(dataset.index)

  def toRuleset(dataset: Dataset)(implicit debugger: Debugger): Ruleset = toRuleset(dataset.index)

  def toRuleset(index: Index): Ruleset = {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    Ruleset(index, rules.map(ResolvedRule.simple(_)).filter(_._2.isEmpty).map(_._1).cached)
  }

}

object Model {

  sealed trait PredictionType

  object PredictionType {

    case object Missing extends PredictionType

    case object Existing extends PredictionType

    case object Complementary extends PredictionType

    case object All extends PredictionType

  }

  def apply(rules: ForEach[ResolvedRule]): Model = new Model(rules, Runtime.getRuntime.availableProcessors())

  def apply(file: File)(implicit reader: RulesetReader): Model = {
    val newReader = if (reader == RulesetReader.NoReader) RulesetReader(file) else reader
    apply(newReader.fromFile(file))
  }

  def apply(file: String)(implicit reader: RulesetReader): Model = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: RulesetReader): Model = apply(reader.fromInputStream(is))

  def fromCache(is: => InputStream): Model = apply(new ForEach[ResolvedRule] {
    def foreach(f: ResolvedRule => Unit): Unit = Deserializer.deserializeFromInputStream[ResolvedRule, Unit](is) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
    }
  })

  def fromCache(file: File): Model = fromCache(new FileInputStream(file))

  def fromCache(file: String): Model = fromCache(new File(file))

  /*def main(args: Array[String]): Unit = {
    val ids = Graph("workspace/trains.sql").filter(_.predicate.hasSameUriAs("train_id")).triples.toVector.groupBy(_.`object`).toVector
    println(ids.size)
    val (train, test) = {
      val (train, test) = ids.splitAt((ids.length * 0.6).toInt)
      train.flatMap(_._2).flatMap(x => List(x.subject, x.`object`)).toSet -> test.flatMap(_._2).flatMap(x => List(x.subject, x.`object`)).toSet
    }
    /*println(train)
    println("**********")
    println(test)*/
    //val traingGraph = Graph("workspace/trains.sql")
    val traingGraph = Graph("workspace/trains.sql").filter(x => train(x.subject)).export("workspace/trains_train.tsv")
    val testGraph = Graph("workspace/trains.sql").filter(x => test(x.subject)).export("workspace/trains_test.tsv")
    /*val rules = traingGraph.mine(Amie().addThreshold(Threshold.MinHeadSize(1)).addThreshold(Threshold.MaxRuleLength(4)).addThreshold(Threshold.TopK(10000)).addConstraint(RuleConstraint.WithInstances(false)).addPattern(RulePattern(AtomPattern(predicate = TripleItem.Uri("direction")))))
    rules.computeConfidence(0.5).sorted.take(100).resolvedRules.foreach(println)
    println("-------")
    val cg = rules.computeConfidence(0.5).sorted.model.completeGraph(testGraph).onlyFunctionalProperties
    val pt = cg.predictedTriples.toVector
    pt.foreach { p =>
      println(s"$p, ${p.rule}")
    }
    println(pt.size)
    println(cg.evaluate)*/
  }*/

}