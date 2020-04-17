package com.github.propi.rdfrules.model

import java.io._

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.ops.{Cacheable, Transformable}
import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.index.{CompressedQuad, Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.rule.{Atom, Measure, ResolvedRulePatternMatcher, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.ruleset.{ResolvedRule, Ruleset, RulesetReader, RulesetWriter}
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.util.Try

/**
  * Created by Vaclav Zeman on 14. 10. 2019.
  */
class Model private(val rules: Traversable[ResolvedRule], val parallelism: Int)
  extends Transformable[ResolvedRule, Model]
    with Cacheable[ResolvedRule, Model]
    with Sortable[ResolvedRule, Model] {

  protected val serializer: Serializer[ResolvedRule] = implicitly[Serializer[ResolvedRule]]
  protected val deserializer: Deserializer[ResolvedRule] = implicitly[Deserializer[ResolvedRule]]
  protected val serializationSize: SerializationSize[ResolvedRule] = implicitly[SerializationSize[ResolvedRule]]
  protected val ordering: Ordering[ResolvedRule] = implicitly[Ordering[ResolvedRule]]

  protected def coll: Traversable[ResolvedRule] = rules

  protected def transform(col: Traversable[ResolvedRule]): Model = new Model(col, parallelism)

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
    val rulePatternMatcher = implicitly[ResolvedRulePatternMatcher[ResolvedRule]]
    filter(rule => allPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule, rulePattern)))
  }

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Model = sortBy { rule =>
    (rule.measures(measure) +: measures.map(rule.measures(_))).asInstanceOf[Iterable[Measure]]
  }

  def sortByRuleLength(measures: Key[Measure]*): Model = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)).asInstanceOf[Iterable[Measure]])
  }

  def foreach(f: ResolvedRule => Unit): Unit = rules.foreach(f)

  def export(os: => OutputStream)(implicit writer: RulesetWriter): Unit = writer.writeToOutputStream(rules, os)

  def export(file: File)(implicit writer: RulesetWriter): Unit = {
    val newWriter = if (writer == RulesetWriter.NoWriter) RulesetWriter(file) else writer
    export(new FileOutputStream(file))(newWriter)
  }

  def export(file: String)(implicit writer: RulesetWriter): Unit = export(new File(file))

  def toRuleset(index: Index): Ruleset = {
    index.tripleItemMap { implicit mapper =>
      Ruleset(index, rules.view.map(ResolvedRule.simple(_)).filter(_._2.isEmpty).map(_._1).toVector)
    }
  }

  def predictForGraph(graph: Graph, predictionType: PredictionType)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = predictForIndex(graph.index(), predictionType)

  def predictForDataset(dataset: Dataset, predictionType: PredictionType)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = predictForIndex(dataset.index(), predictionType)

  def predictForIndex(index: Index, predictionType: PredictionType): PredictionResult = PredictionResult(
    new Traversable[PredictedTriple] {
      def foreach[U](f: PredictedTriple => U): Unit = {
        index.tripleItemMap { mapper =>
          index.tripleMap { implicit thi =>
            val atomCounting = new AtomCounting {
              implicit val tripleIndex: TripleHashIndex[Int] = thi
            }
            val filterPredictedTriple: PredictedTriple => Boolean = predictionType match {
              case PredictionType.All => _ => true
              case PredictionType.Existing => _.existing
              case PredictionType.Missing => !_.existing
            }
            rules.view.map(ResolvedRule.simple(_)(mapper)).foreach { case (rule, ruleMapper) =>
              implicit val mapper2: TripleItemHashIndex = mapper.extendWith(ruleMapper)
              val ruleBody = rule.body.toSet
              val headVars = List(rule.head.subject, rule.head.`object`).collect {
                case x: Atom.Variable => x
              }
              val constantsToQuad: Seq[Atom.Constant] => CompressedQuad = (rule.head.subject, rule.head.`object`) match {
                case (_: Atom.Variable, _: Atom.Variable) => constants => CompressedQuad(constants.head.value, rule.head.predicate, constants.last.value, 0)
                case (_: Atom.Variable, Atom.Constant(o)) => constants => CompressedQuad(constants.head.value, rule.head.predicate, o, 0)
                case (Atom.Constant(s), _: Atom.Variable) => constants => CompressedQuad(s, rule.head.predicate, constants.head.value, 0)
                case (Atom.Constant(s), Atom.Constant(o)) => _ => CompressedQuad(s, rule.head.predicate, o, 0)
              }
              Try(atomCounting
                .selectDistinctPairs(ruleBody, headVars, new atomCounting.VariableMap(true))
                .map(constantsToQuad)
                .map(x => thi.predicates.get(x.predicate).flatMap(_.subjects.get(x.subject)).exists(_.contains(x.`object`)) -> x.toTriple)
                .map(x => PredictedTriple(x._2)(rule, x._1))
                .filter(filterPredictedTriple)
                .foreach(f))
            }
          }
        }
      }
    },
    index
  )

}

object Model {

  sealed trait PredictionType

  object PredictionType {

    case object Missing extends PredictionType

    case object Existing extends PredictionType

    case object All extends PredictionType

  }

  def apply(rules: Traversable[ResolvedRule]): Model = new Model(rules, Runtime.getRuntime.availableProcessors())

  def apply(file: File)(implicit reader: RulesetReader): Model = {
    val newReader = if (reader == RulesetReader.NoReader) RulesetReader(file) else reader
    apply(newReader.fromFile(file))
  }

  def apply(file: String)(implicit reader: RulesetReader): Model = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: RulesetReader): Model = apply(reader.fromInputStream(is))

  def fromCache(is: => InputStream): Model = apply(new Traversable[ResolvedRule] {
    def foreach[U](f: ResolvedRule => U): Unit = Deserializer.deserializeFromInputStream[ResolvedRule, Unit](is) { reader =>
      Stream.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
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