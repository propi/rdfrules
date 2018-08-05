package com.github.propi.rdfrules.ruleset

import java.io._

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.data.ops.{Cacheable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.{Measure, Rule, RulePattern, RulePatternMatcher}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.collection.mutable

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset private(val rules: Traversable[Rule.Simple], val index: Index)
  extends Transformable[ResolvedRule, Ruleset]
    with Cacheable[ResolvedRule, Ruleset]
    with Sortable[ResolvedRule, Ruleset] {

  protected def coll: Traversable[ResolvedRule] = new Traversable[ResolvedRule] {
    def foreach[U](f: ResolvedRule => U): Unit = index.tripleItemMap { implicit mapper =>
      rules.foreach(x => f(x))
    }
  }

  protected def transformSimple(col: Traversable[Rule.Simple]): Ruleset = new Ruleset(col, index)

  protected def transform(col: Traversable[ResolvedRule]): Ruleset = transformSimple(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = index.tripleItemMap { implicit mapper =>
      col.foreach(x => f(x))
    }
  })

  protected val ordering: Ordering[ResolvedRule] = implicitly[Ordering[ResolvedRule]]
  protected val serializer: Serializer[ResolvedRule] = implicitly[Serializer[ResolvedRule]]
  protected val deserializer: Deserializer[ResolvedRule] = new Deserializer[ResolvedRule] {
    def deserialize(v: Array[Byte]): ResolvedRule = ???
  }
  protected val serializationSize: SerializationSize[ResolvedRule] = new SerializationSize[ResolvedRule] {
    val size: Int = implicitly[SerializationSize[Rule.Simple]].size
  }

  def filter(pattern: RulePattern, patterns: RulePattern*): Ruleset = transformSimple(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = index.tripleItemMap { implicit mapper =>
      index.tripleMap { implicit thi =>
        val rulePatternMatcher = implicitly[RulePatternMatcher[Rule]]
        val mappedPatterns = (pattern +: patterns).map(_.mapped)
        rules.view.filter(rule => mappedPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule, rulePattern))).foreach(f)
      }
    }
  })

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.measures(measure) +: measures.map(rule.measures(_))).asInstanceOf[Iterable[Measure]]
  }

  def sortByRuleLength(measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)).asInstanceOf[Iterable[Measure]])
  }

  def resolvedRules: Traversable[ResolvedRule] = coll

  def foreach(f: ResolvedRule => Unit): Unit = coll.foreach(f)

  private def extendRuleset(f: TripleHashIndex => Rule.Simple => Rule.Simple): Ruleset = transformSimple(new Traversable[Rule.Simple] {
    def foreach[U](f2: Rule.Simple => U): Unit = index.tripleMap { thi =>
      rules.view.map(f(thi)(_)).foreach(f2)
    }
  })

  def graphBasedRules: Ruleset = extendRuleset { implicit thi =>
    rule => Rule.Simple(rule.head.toGraphBasedAtom, rule.body.map(_.toGraphBasedAtom))(rule.measures)
  }

  def countConfidence(minConfidence: Double): Ruleset = extendRuleset { implicit thi =>
    _.withConfidence(minConfidence)
  }.filter(_.measures.get[Measure.Confidence].exists(_.value >= minConfidence))

  def countPcaConfidence(minPcaConfidence: Double): Ruleset = extendRuleset { implicit thi =>
    _.withPcaConfidence(minPcaConfidence)
  }.filter(_.measures.get[Measure.PcaConfidence].exists(_.value >= minPcaConfidence))

  def countLift(minConfidence: Double = 0.5): Ruleset = extendRuleset { implicit thi =>
    Function.chain[Rule.Simple](List(
      rule => if (rule.measures.exists[Measure.Confidence]) rule else rule.withConfidence(minConfidence),
      rule => if (rule.measures.exists[Measure.HeadConfidence]) rule else rule.withHeadConfidence,
      rule => rule.withLift
    ))
  }.filter(_.measures.exists[Measure.Lift])

  def countClusters(clustering: Clustering[Rule.Simple]): Ruleset = transformSimple(new Traversable[Rule.Simple] {
    def foreach[U](f: Rule.Simple => U): Unit = clustering.clusters(rules.toIndexedSeq).view.zipWithIndex.flatMap { case (cluster, index) =>
      cluster.map(x => x.copy()(TypedKeyMap(Measure.Cluster(index)) ++= x.measures))
    }.foreach(f)
  })

  def findSimilar(rule: ResolvedRule, k: Int, dissimilar: Boolean = false)(implicit simf: SimilarityCounting[Rule.Simple]): Ruleset = if (k < 1) {
    findSimilar(rule, 1, dissimilar)
  } else {
    transformSimple(new Traversable[Rule.Simple] {
      def foreach[U](f: Rule.Simple => U): Unit = {
        val ruleSimple = index.tripleItemMap { implicit mapper =>
          Rule.Simple(rule)
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

  def export[T <: RulesetSource](os: => OutputStream)(implicit writer: RulesetWriter[T]): Unit = writer.writeToOutputStream(this, os)

  def export[T <: RulesetSource](file: File)(implicit writer: RulesetWriter[T]): Unit = export(new FileOutputStream(file))

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

}
