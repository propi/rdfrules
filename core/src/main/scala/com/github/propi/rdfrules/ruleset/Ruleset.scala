package com.github.propi.rdfrules.ruleset

import java.io._

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.data.ops.{Cacheable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleHashIndex}
import com.github.propi.rdfrules.rule.{Measure, Rule, RulePattern, RulePatternMatcher}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.{Debugger, TypedKeyMap}
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.collection.mutable

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset private(val rules: Traversable[Rule.Simple], val index: Index)
  extends Transformable[Rule.Simple, Ruleset]
    with Cacheable[Rule.Simple, Ruleset]
    with Sortable[Rule.Simple, Ruleset] {

  self =>

  protected def coll: Traversable[Rule.Simple] = rules

  protected def transform(col: Traversable[Rule.Simple]): Ruleset = new Ruleset(col, index)

  protected val ordering: Ordering[Rule.Simple] = implicitly[Ordering[Rule.Simple]]
  protected val serializer: Serializer[Rule.Simple] = implicitly[Serializer[Rule.Simple]]
  protected val deserializer: Deserializer[Rule.Simple] = implicitly[Deserializer[Rule.Simple]]
  protected val serializationSize: SerializationSize[Rule.Simple] = implicitly[SerializationSize[Rule.Simple]]

  def filter(pattern: RulePattern, patterns: RulePattern*): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        val rulePatternMatcher = implicitly[RulePatternMatcher[Rule]]
        val mappedPatterns = (pattern +: patterns).map(_.mapped)
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

  def headResolved: ResolvedRule = resolvedRules.head

  def headResolvedOption: Option[ResolvedRule] = resolvedRules.headOption

  def findResolved(f: ResolvedRule => Boolean): Option[ResolvedRule] = resolvedRules.find(f)

  private def extendRuleset(f: TripleHashIndex => Rule.Simple => Rule.Simple)(implicit debugger: Int => (Debugger.ActionDebugger => Unit) => Unit): Ruleset = transform(new Traversable[Rule.Simple] {
    def foreach[U](f2: Rule.Simple => U): Unit = index.tripleMap { thi =>
      val cached = self.cache
      debugger(cached.size) { ad =>
        cached.rules.view.map(f(thi)(_)).foreach(x => ad.result()(f2(x)))
      }
    }
  })

  def graphBasedRules: Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => Debugger.EmptyDebugger.debug("", size)(f)
    extendRuleset { implicit thi =>
      rule => Rule.Simple(rule.head.toGraphBasedAtom, rule.body.map(_.toGraphBasedAtom))(rule.measures)
    }
  }

  def computeConfidence(minConfidence: Double)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("Confidence computing", size)(f)
    extendRuleset { implicit thi =>
      _.withConfidence(minConfidence)
    }.filter(_.measures.get[Measure.Confidence].exists(_.value >= minConfidence))
  }

  def computePcaConfidence(minPcaConfidence: Double)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("PCA Confidence computing", size)(f)
    extendRuleset { implicit thi =>
      _.withPcaConfidence(minPcaConfidence)
    }.filter(_.measures.get[Measure.PcaConfidence].exists(_.value >= minPcaConfidence))
  }

  def computeLift(minConfidence: Double = 0.5)(implicit debugger: Debugger): Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => debugger.debug("Lift computing", size)(f)
    extendRuleset { implicit thi =>
      Function.chain[Rule.Simple](List(
        rule => if (rule.measures.exists[Measure.Confidence]) rule else rule.withConfidence(minConfidence),
        rule => if (rule.measures.exists[Measure.HeadConfidence]) rule else rule.withHeadConfidence,
        rule => rule.withLift
      ))
    }.filter(_.measures.exists[Measure.Lift])
  }

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

}

object Ruleset {

  def apply(rules: IndexedSeq[Rule.Simple], index: Index): Ruleset = new Ruleset(rules, index)

  def fromCache(index: Index, is: => InputStream): Ruleset = new Ruleset(
    new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = Deserializer.deserializeFromInputStream[Rule.Simple, Unit](is) { reader =>
        Stream.continually(reader.read()).takeWhile(_.isDefined).map(_.get).foreach(f)
      }
    },
    index
  )

  def fromCache(index: Index, file: File): Ruleset = fromCache(index, new FileInputStream(file))

  def fromCache(index: Index, file: String): Ruleset = fromCache(index, new File(file))

}