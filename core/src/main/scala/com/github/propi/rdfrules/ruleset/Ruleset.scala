package com.github.propi.rdfrules.ruleset

import java.io._

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleIndex}
import com.github.propi.rdfrules.model.Model.PredictionType
import com.github.propi.rdfrules.model.{Model, PredictionResult}
import com.github.propi.rdfrules.rule.{Measure, PatternMatcher, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.{Sortable, Treeable}
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.extensions.IterableOnceExtension.PimpedIterableOnce
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}
import com.github.propi.rdfrules.utils.workers.Workers._
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import com.github.propi.rdfrules.rule.RulePatternMatcher._

import scala.collection.mutable

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset private(val rules: Traversable[Rule.Simple], val index: Index, val parallelism: Int, val isCached: Boolean)
  extends Transformable[Rule.Simple, Ruleset]
    with Cacheable[Rule.Simple, Ruleset]
    with Sortable[Rule.Simple, Ruleset]
    with Debugable[Rule.Simple, Ruleset]
    with Treeable {

  self =>

  protected def coll: Traversable[Rule.Simple] = rules

  protected def transform(col: Traversable[Rule.Simple]): Ruleset = new Ruleset(col, index, parallelism, isCached)

  protected def cachedTransform(col: Traversable[Rule.Simple]): Ruleset = new Ruleset(col, index, parallelism, true)

  protected val ordering: Ordering[Rule.Simple] = implicitly[Ordering[Rule.Simple]]
  protected val serializer: Serializer[Rule.Simple] = implicitly[Serializer[Rule.Simple]]
  protected val deserializer: Deserializer[Rule.Simple] = implicitly[Deserializer[Rule.Simple]]
  protected val serializationSize: SerializationSize[Rule.Simple] = implicitly[SerializationSize[Rule.Simple]]
  protected val dataLoadingText: String = "Ruleset loading"

  def withIndex(index: Index): Ruleset = new Ruleset(rules, index, parallelism, isCached)

  def filter(pattern: RulePattern, patterns: RulePattern*): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
        val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
        rules.view.filter(rule => mappedPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule, rulePattern))).foreach(f)
      }
    }
  })

  def filterResolved(f: ResolvedRule => Boolean): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f2: Rule.Simple => U): Unit = index.tripleItemMap { implicit mapper =>
      rules.view.filter(x => f(x)).foreach(f2)
    }
  })

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.measures(measure) +: measures.map(rule.measures(_))).asInstanceOf[Iterable[Measure]]
  }

  def sortByResolved[A](f: ResolvedRule => A)(implicit ord: Ordering[A]): Ruleset = index.tripleItemMap { implicit mapper =>
    sortBy(x => f(x))
  }

  def sortByRuleLength(measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)).asInstanceOf[Iterable[Measure]])
  }

  def resolvedRules: Traversable[ResolvedRule] = new Traversable[ResolvedRule] {
    def foreach[U](f: ResolvedRule => U): Unit = index.tripleItemMap { implicit mapper =>
      rules.foreach(x => f(x))
    }
  }

  def foreach(f: ResolvedRule => Unit): Unit = resolvedRules.foreach(f)

  def instantiate(part: CoveredPaths.Part = CoveredPaths.Part.Whole, allowDuplicateAtoms: Boolean = true): Traversable[CoveredPaths] = rules.view.map(CoveredPaths(_, part, index, allowDuplicateAtoms))

  def +(ruleset: Ruleset): Ruleset = transform(rules.concat(ruleset.rules))

  def headResolved: ResolvedRule = resolvedRules.head

  def headResolvedOption: Option[ResolvedRule] = resolvedRules.headOption

  def findResolved(f: ResolvedRule => Boolean): Option[ResolvedRule] = resolvedRules.find(f)

  def model: Model = Model(resolvedRules.toVector, true).setParallelism(parallelism)

  /**
    * Prune rules with CBA strategy
    *
    * @param onlyExistingTriples      if true the common CBA strategy will be used. That means we take only such predicted triples (of the rules),
    *                                 which are contained in the input dataset. This strategy takes maximally as much memory as the number of triples
    *                                 in the input dataset. If false we take all predicted triples (including triples which are not contained in the
    *                                 input dataset and are newly generated). For deduplication a HashSet is used and therefore the memory may increase
    *                                 unexpectedly because we need to save all unique generated triples into memory.
    * @param onlyFunctionalProperties if true the predicted triples are deduplicated by (subject, predicate). E.g. if some triple (A B C) is generated
    *                                 for some rule then next generated triples with form (A B *) are skipped. We expect only functional properties;
    *                                 it means the tuple (subject, predicate) can have only one object. If you expect non function properties, set
    *                                 this parameter to false.
    * @return pruned ruleset
    */
  def pruned(onlyExistingTriples: Boolean, onlyFunctionalProperties: Boolean): Ruleset = {
    transform(new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = {
        index.tripleItemMap { implicit mapper =>
          val predictionType = if (onlyExistingTriples) PredictionType.Existing else PredictionType.All
          val predictionResult = if (onlyFunctionalProperties) predictedTriples(predictionType).onlyFunctionalProperties else predictedTriples(predictionType).distinct
          val hashSet = collection.mutable.LinkedHashSet.empty[Rule.Simple]
          for (rule <- predictionResult.predictedTriples.map(_.rule).map(ResolvedRule.simple(_)).filter(_._2.isEmpty).map(_._1)) {
            hashSet += rule
          }
          hashSet.foreach(f)
        }
      }
    })
  }

  private def mapRuleset(f: TripleIndex[Int] => Rule.Simple => Rule.Simple)(implicit debugger: Int => (Debugger.ActionDebugger => Unit) => Unit): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f2: Rule.Simple => U): Unit = index.tripleMap { thi =>
      val cached = self.cache
      debugger(cached.size) { ad =>
        //here we use parallel mapping
        cached.rules.toIndexedSeq.parMap(parallelism) { rule =>
          ad.result()(f(thi)(rule))
        }.foreach(x => f2(x))
      }
    }
  })

  private def shrinkRuleset[T](topK: Int, initThreshold: T, updateThreshold: Rule.Simple => T)(f: TripleIndex[Int] => (Rule.Simple, T) => Rule.Simple)(implicit ord: Ordering[Rule.Simple], debugger: Int => (Debugger.ActionDebugger => Unit) => Unit): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f2: Rule.Simple => U): Unit = index.tripleMap { thi =>
      val cached = self.cache
      debugger(cached.size) { ad =>
        //here we use topK parallel enumeration with some initThreshold and with a function which updates threshold once the queue head is changed
        cached.rules.toIndexedSeq.topK(topK, initThreshold, updateThreshold, parallelism) { (rule, threshold) =>
          Iterator(ad.result()(f(thi)(rule, threshold)))
        }.foreach(x => f2(x))
      }
    }
  })

  def graphBasedRules: Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => Debugger.EmptyDebugger.debug("", size)(f)
    transform(new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = index.tripleMap { implicit tripleMap =>
        rules.view.map(rule => Rule.Simple(rule.head.toGraphBasedAtom, rule.body.map(_.toGraphBasedAtom))(rule.measures)).foreach(f)
      }
    })
  }

  def computeConfidence(minConfidence: Double, topK: Int = 0)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("Confidence computing", size)(f)
    val newRuleset = if (topK > 0) {
      //if we use topK approach then the final ruleset will have size lower than or equals to the original size
      //therefore we shrink the original ruleset
      //first we need to define rule ordering for priority queue
      implicit val ord: Ordering[Rule.Simple] = Ordering.by[Rule.Simple, (Double, Double)](x => x.measures.get[Measure.Confidence].map(_.value).getOrElse(0.0) -> x.measures[Measure.HeadCoverage].value).reverse
      shrinkRuleset(topK, minConfidence, _.measures.get[Measure.Confidence].map(_.value).getOrElse(minConfidence)) { implicit thi =>
        (rule, threshold) => rule.withConfidence(threshold)
      }
    } else {
      //otherwise we only map each rule to another with computed confidence
      mapRuleset { implicit thi =>
        _.withConfidence(minConfidence)
      }
    }
    newRuleset.filter(_.measures.get[Measure.Confidence].exists(_.value >= minConfidence))
  }

  def computePcaConfidence(minPcaConfidence: Double, topK: Int = 0)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("PCA Confidence computing", size)(f)
    val newRuleset = if (topK > 0) {
      //if we use topK approach then the final ruleset will have size lower than or equals to the original size
      //therefore we shrink the original ruleset
      //first we need to define rule ordering for priority queue
      implicit val ord: Ordering[Rule.Simple] = Ordering.by[Rule.Simple, (Double, Double)](x => x.measures.get[Measure.PcaConfidence].map(_.value).getOrElse(0.0) -> x.measures[Measure.HeadCoverage].value).reverse
      shrinkRuleset(topK, minPcaConfidence, _.measures.get[Measure.PcaConfidence].map(_.value).getOrElse(minPcaConfidence)) { implicit thi =>
        (rule, threshold) => rule.withPcaConfidence(threshold)
      }
    } else {
      //otherwise we only map each rule to another with computed confidence
      mapRuleset { implicit thi =>
        _.withPcaConfidence(minPcaConfidence)
      }
    }
    newRuleset.filter(_.measures.get[Measure.PcaConfidence].exists(_.value >= minPcaConfidence))
  }

  def computeLift(minConfidence: Double = 0.5)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("Lift computing", size)(f)
    mapRuleset { implicit thi =>
      Function.chain[Rule.Simple](List(
        rule => if (rule.measures.exists[Measure.Confidence]) rule else rule.withConfidence(minConfidence),
        rule => if (rule.measures.exists[Measure.HeadConfidence]) rule else rule.withHeadConfidence,
        rule => rule.withLift
      ))
    }.filter(_.measures.exists[Measure.Lift])
  }

  /**
    * Get all predicted triples by rules. Predicted rules have two types - Existing and Missing. If a generated triple has Existing
    * type, the triple also exists in the input dataset. If Missing, the triple is not contained in the input dataset. We can choose
    * which type of predicted rules is desired.
    *
    * @param predictionType Existing = all predicted triples are contained in the input datasets
    *                       Missing = all predicted triples are not contained in the input datasets
    *                       All = predicted triples can be Existing or Missing
    *                       Complementary = predicted triple is Missing and the subject did not contain any information related with the predicted predicate (it is new valuable knowledge)
    * @return predicted triples
    */
  def predictedTriples(predictionType: PredictionType): PredictionResult = model.predictForIndex(index, predictionType)

  def makeClusters(clustering: Clustering[Rule.Simple]): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = clustering.clusters(rules.toIndexedSeq).view.zipWithIndex.flatMap { case (cluster, index) =>
      cluster.map(x => x.copy()(TypedKeyMap(Measure.Cluster(index)) ++= x.measures))
    }.foreach(f)
  })

  def findSimilar(rule: ResolvedRule, k: Int, dissimilar: Boolean = false)(implicit simf: SimilarityCounting[Rule.Simple]): Ruleset = if (k < 1) {
    findSimilar(rule, 1, dissimilar)
  } else {
    transform(new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = {
        val ruleSimple = index.tripleItemMap { implicit mapper =>
          ResolvedRule.simple(rule)._1
        }
        val ordering = Ordering.by[(Double, Rule.Simple), Double](_._1)
        val queue = mutable.PriorityQueue.empty(if (dissimilar) ordering else ordering.reverse)
        for (rule2 <- rules if rule2 != ruleSimple) {
          val sim = simf(ruleSimple, rule2)
          if (queue.size < k) {
            queue.enqueue(sim -> rule2)
          } else if ((!dissimilar && sim > queue.head._1) || (dissimilar && sim < queue.head._1)) {
            queue.dequeue()
            queue.enqueue(sim -> rule2)
          }
        }
        queue.dequeueAll.reverseIterator.map(_._2).foreach(f)
      }
    })
  }

  def findDissimilar(rule: ResolvedRule, k: Int)(implicit simf: SimilarityCounting[Rule.Simple]): Ruleset = findSimilar(rule, k, true)

  def export(os: => OutputStream)(implicit writer: RulesetWriter): Unit = writer.writeToOutputStream(this, os)

  def export(file: File)(implicit writer: RulesetWriter): Unit = {
    val newWriter = if (writer == RulesetWriter.NoWriter) RulesetWriter(file) else writer
    export(new FileOutputStream(file))(newWriter)
  }

  def export(file: String)(implicit writer: RulesetWriter): Unit = export(new File(file))

  /**
    * Set number of workers for parallel tasks (confidences computing)
    * The parallelism should be equal to or lower than the max thread pool size of the execution context
    *
    * @param parallelism number of workers
    * @return
    */
  def setParallelism(parallelism: Int): Ruleset = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    new Ruleset(rules, index, normParallelism, isCached)
  }

}

object Ruleset {

  def apply(index: Index, rules: Traversable[Rule.Simple], isCached: Boolean): Ruleset = new Ruleset(rules, index, Runtime.getRuntime.availableProcessors(), isCached)

  def apply(index: Index, file: File)(implicit reader: RulesetReader): Ruleset = Model(file).toRuleset(index)

  def apply(index: Index, file: String)(implicit reader: RulesetReader): Ruleset = apply(index, new File(file))

  def apply(index: Index, is: => InputStream)(implicit reader: RulesetReader): Ruleset = Model(is).toRuleset(index)

  def fromCache(index: Index, is: => InputStream): Ruleset = new Ruleset(
    new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = Deserializer.deserializeFromInputStream[Rule.Simple, Unit](is) { reader =>
        Stream.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
      }
    },
    index,
    Runtime.getRuntime.availableProcessors(),
    false
  )

  def fromCache(index: Index, file: File): Ruleset = fromCache(index, new FileInputStream(file))

  def fromCache(index: Index, file: String): Ruleset = fromCache(index, new File(file))

}