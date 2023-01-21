package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting
import com.github.propi.rdfrules.algorithm.clustering.SimilarityCounting._
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule extends RuleContent {
  def measures: TypedKeyMap[Measure]

  def support: Int

  /**
    * If the support is approximative then support = ~support (time-windowed) * supportIncreaseRatio
    * SupportIncreaseRatio > 0 indicates that the support increase ratio was used and support is approximative.
    * support = ~support (time-windowed) * supportIncreaseRatio
    * ~support (time-windowed) = support / supportIncreaseRatio
    * samples = headSupport / supportIncreaseRatio
    * ~hc = ~support / samples
    *
    * @return
    */
  def supportIncreaseRatio: Float

  def headSize: Int

  def headSupport: Int

  def headCoverage: Double

  override def hashCode(): Int = {
    super.hashCode() + this.headSupport // * 2 + this.support
  }

  override def equals(other: Any): Boolean = other match {
    case rule: Rule if headSupport == rule.headSupport && (support == rule.support || (supportIncreaseRatio + rule.supportIncreaseRatio) > 0.0f) => super.equals(other)
    case _ => false
  }
}

object Rule {

  sealed trait FinalRule extends Rule {
    def measures: TypedKeyMap.Immutable[Measure]

    def withMeasures(measure: TypedKeyMap.Immutable[Measure]): FinalRule

    def withContent(head: Atom = this.head, body: IndexedSeq[Atom] = this.body): FinalRule

    override lazy val bodySet: Set[Atom] = body.toSet
  }

  private case class Simple(head: Atom, body: IndexedSeq[Atom])(val measures: TypedKeyMap.Immutable[Measure]) extends FinalRule {
    def support: Int = measures[Measure.Support].value

    def headSupport: Int = measures[Measure.HeadSupport].value

    def supportIncreaseRatio: Float = measures.get[Measure.SupportIncreaseRatio].map(_.value).getOrElse(0.0f)

    def headSize: Int = measures[Measure.HeadSize].value

    def headCoverage: Double = measures[Measure.HeadCoverage].value

    def withMeasures(measure: TypedKeyMap.Immutable[Measure]): FinalRule = copy()(measure)

    def withContent(head: Atom, body: IndexedSeq[Atom]): FinalRule = Simple(head, body)(measures)
  }

  def apply(head: Atom, body: IndexedSeq[Atom], measures: TypedKeyMap.Mutable[Measure]): FinalRule = {
    val measuresMap = Function.chain[TypedKeyMap.Mutable[Measure]](List(
      m => if (m.exists[Measure.Support]) m else m += Measure.Support(0),
      m => if (m.exists[Measure.HeadSize]) m else m += Measure.HeadSize(0),
      m => if (m.exists[Measure.HeadSupport]) m else m += Measure.HeadSupport(0),
      m => if (m.exists[Measure.HeadCoverage]) m else m += Measure.HeadCoverage(0.0)
    ))(measures)
    Simple(head, body)(measuresMap.toImmutable)
  }

  def apply(head: Atom, body: IndexedSeq[Atom], measures: TypedKeyMap.Immutable[Measure]): FinalRule = {
    val measuresMap = Function.chain[TypedKeyMap.Immutable[Measure]](List(
      m => if (m.exists[Measure.Support]) m else m + Measure.Support(0),
      m => if (m.exists[Measure.HeadSize]) m else m + Measure.HeadSize(0),
      m => if (m.exists[Measure.HeadSupport]) m else m + Measure.HeadSupport(0),
      m => if (m.exists[Measure.HeadCoverage]) m else m + Measure.HeadCoverage(0.0)
    ))(measures)
    Simple(head, body)(measuresMap)
  }

  def apply(head: Atom, body: IndexedSeq[Atom], measures: Measure*): FinalRule = apply(head, body, TypedKeyMap.Mutable(measures))

  /*implicit def apply(rule: Rule): FinalRule = rule match {
    case x: FinalRule => x
    case _ =>
      val measures = TypedKeyMap(
        Measure.Support(rule.support),
        Measure.HeadSize(rule.headSize),
        Measure.HeadCoverage(rule.headCoverage),
        Measure.HeadSupport(rule.headSupport)
      )
      Simple(rule.head, rule.body)(if (rule.supportIncreaseRatio > 0.0f) measures += Measure.SupportIncreaseRatio(rule.supportIncreaseRatio) else measures)
  }*/

  implicit def ruleOrdering(implicit measuresOrdering: Ordering[TypedKeyMap[Measure]]): Ordering[Rule] = Ordering.by[Rule, TypedKeyMap[Measure]](_.measures)

  implicit def ruleSimpleOrdering(implicit measuresOrdering: Ordering[TypedKeyMap[Measure]]): Ordering[FinalRule] = Ordering.by[FinalRule, Rule](_.asInstanceOf[Rule])

  implicit val ruleSimilarityCounting: SimilarityCounting[Rule] = AllAtomsSimilarityCounting /*(0.5 * AtomsSimilarityCounting) ~
    (0.1 * LengthSimilarityCounting) ~
    (0.15 * SupportSimilarityCounting) ~
    (0.05 * ConfidenceSimilarityCounting) ~
    (0.15 * PcaConfidenceSimilarityCounting) ~
    (0.05 * LiftSimilarityCounting)*/

  /*implicit def ruleStringifier(implicit ruleSimpleStringifier: Stringifier[FinalRule]): Stringifier[Rule] = (v: Rule) => ruleSimpleStringifier.toStringValue(v match {
    case x: Simple => x
    case x: ExpandingRule => Simple(x.head, x.body)(TypedKeyMap())
    case x => Simple(x.head, x.body)(x.measures)
  })*/

  implicit def ruleSimpleStringifier(implicit resolvedRuleStringifier: Stringifier[ResolvedRule], mapper: TripleItemIndex): Stringifier[FinalRule] = (v: FinalRule) => resolvedRuleStringifier.toStringValue(v)

}