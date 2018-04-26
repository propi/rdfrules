package com.github.propi.rdfrules.ruleset

import java.io.{InputStream, OutputStream}

import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting._
import com.github.propi.rdfrules.algorithm.dbscan.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.data.ops.{Cacheable, Transformable}
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.{Measure, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.Sortable
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.stringifier.Stringifier
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}
import com.github.propi.rdfrules.stringifier.CommonStringifiers._

import scala.collection.GenSeq

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset private(val rules: Seq[Rule.Simple], val index: Index)
  extends Transformable[Rule.Simple, Ruleset]
    with Cacheable[Rule.Simple, Ruleset]
    with Sortable[Rule.Simple, Ruleset] {

  protected def seqColl: Seq[Rule.Simple] = rules.view

  protected def coll: Traversable[Rule.Simple] = seqColl

  protected def transform(col: Traversable[Rule.Simple]): Ruleset = col match {
    case simples: Seq[Rule.Simple] => new Ruleset(simples, index)
    case _ => new Ruleset(col.toVector, index)
  }

  protected val ordering: Ordering[Rule.Simple] = implicitly[Ordering[Rule.Simple]]
  protected val serializer: Serializer[Rule.Simple] = implicitly[Serializer[Rule.Simple]]
  protected val deserializer: Deserializer[Rule.Simple] = implicitly[Deserializer[Rule.Simple]]
  protected val serializationSize: SerializationSize[Rule.Simple] = implicitly[SerializationSize[Rule.Simple]]

  def filterByPatterns(pattern: RulePattern, patterns: RulePattern*): Ruleset = {
    val ruleView = rules.view
    val newRules = new ruleView.Filtered {
      protected[this] val pred: A => Boolean = _
    }
    index.tripleMap { implicit thi =>
      (pattern +: patterns).exists(rulePattern => )
    }
  }

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.measures(measure) +: measures.map(rule.measures(_))).asInstanceOf[Iterable[Measure]]
  }

  def sortByRuleLength(measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)).asInstanceOf[Iterable[Measure]])
  }

  def foreach(f: Rule.Simple => Unit): Unit = coll.foreach(f)

  def resolvedRules: Traversable[ResolvedRule] = new Traversable[ResolvedRule] {
    def foreach[U](f: ResolvedRule => U): Unit = index.tripleItemMap { implicit tihi =>
      coll.map(ResolvedRule.apply).foreach(f)
    }
  }

  def useStringifier(f: Stringifier[Rule.Simple] => Ruleset => Unit): Unit = index.tripleItemMap { implicit tihi =>
    f(implicitly[Stringifier[Rule.Simple]])(this)
  }

  def useMapper(f: TripleItemHashIndex => Ruleset => Unit): Unit = index.tripleItemMap(f(_)(this))

  private def extendRuleset(f: TripleHashIndex => Rule.Simple => Rule.Simple): Ruleset = {
    val ruleView = rules.view
    val newRules = new ruleView.Forced[Rule.Simple] {
      protected[this] lazy val forced: GenSeq[Rule.Simple] = index.tripleMap { thi =>
        ruleView.map(f(thi)(_)).force
      }
    }
    new Ruleset(newRules, index)
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

  def countPcaLift(minPcaConfidence: Double = 0.5): Ruleset = extendRuleset { implicit thi =>
    Function.chain[Rule.Simple](List(
      rule => if (rule.measures.exists[Measure.PcaConfidence]) rule else rule.withPcaConfidence(minPcaConfidence),
      rule => if (rule.measures.exists[Measure.HeadConfidence]) rule else rule.withHeadConfidence,
      rule => rule.withPcaLift
    ))
  }.filter(_.measures.exists[Measure.PcaLift])

  def countClusters(minNeighbours: Int = 5,
                    minSimilarity: Double = 0.9,
                    similarityCounting: SimilarityCounting = (0.6 * AtomsSimilarityCounting) ~ (0.1 * LengthSimilarityCounting) ~ (0.15 * SupportSimilarityCounting) ~ (0.15 * ConfidenceSimilarityCounting)): Ruleset = {
    val rules = DbScan(minNeighbours, minSimilarity, this.rules.toIndexedSeq)(similarityCounting.apply).view.zipWithIndex.flatMap { case (cluster, index) =>
      cluster.map(x => x.copy()(TypedKeyMap(Measure.Cluster(index)) ++= x.measures))
    }
    transform(rules)
  }

  def export[T <: RulesetSource](os: => OutputStream)(implicit writer: RulesetWriter[T]): Unit = writer.writeToOutputStream(this, os)

}

object Ruleset {

  def apply(rules: Seq[Rule.Simple], index: Index): Ruleset = new Ruleset(rules.view, index)

  def fromCache(index: Index)(is: => InputStream): Ruleset = {
    val rules = Deserializer.deserializeFromInputStream[Rule.Simple, IndexedSeq[Rule.Simple]](is) { reader =>
      Stream.continually(reader.read()).takeWhile(_.isDefined).map(_.get).toIndexedSeq
    }
    apply(rules, index)
  }

}
