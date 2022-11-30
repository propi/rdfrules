package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting._
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule {

  val body: IndexedSeq[Atom]
  val head: Atom

  def measures: TypedKeyMap.Immutable[Measure]

  def support: Int

  def headSize: Int

  def headCoverage: Double

  def ruleLength: Int = body.size + 1

  override def hashCode(): Int = {
    val support = this.support
    val headSize = this.headSize
    val bodyHashCode = body.iterator.map { atom =>
      atom.predicate +
        (atom.subject match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        }) +
        (atom.`object` match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        })
    }.foldLeft(0)(_ ^ _)
    (bodyHashCode * body.size * 31) + headSize * 2 + support
  }

}

object Rule {

  sealed trait FinalRule extends Rule {
    def withMeasures(measure: TypedKeyMap.Immutable[Measure]): FinalRule

    private lazy val bodySet = body.toSet

    override def equals(obj: Any): Boolean = obj match {
      case that: Rule => ExpandingRule.checkRuleContentsEquality(that.body, bodySet, that.head, head)
      case _ => false
    }
  }

  private case class Simple(head: Atom, body: IndexedSeq[Atom])(val measures: TypedKeyMap.Immutable[Measure]) extends FinalRule {
    def support: Int = measures[Measure.Support].value

    def headSize: Int = measures[Measure.HeadSize].value

    def headCoverage: Double = measures[Measure.HeadCoverage].value

    def withMeasures(measure: TypedKeyMap.Immutable[Measure]): FinalRule = copy()(measure)
  }

  def apply(head: Atom, body: IndexedSeq[Atom])(measures: TypedKeyMap.Immutable[Measure]): FinalRule = {
    val measuresMap = Function.chain[TypedKeyMap.Immutable[Measure]](List(
      m => if (m.exists[Measure.Support]) m else m + Measure.Support(0),
      m => if (m.exists[Measure.HeadSize]) m else m + Measure.HeadSize(0),
      m => if (m.exists[Measure.HeadCoverage]) m else m + Measure.HeadCoverage(0.0)
    ))(measures)
    Simple(head, body)(measuresMap)
  }

  def apply(head: Atom, body: IndexedSeq[Atom], measures: Measure*): FinalRule = {
    val measuresMap = Function.chain[TypedKeyMap[Measure]](List(
      m => if (m.exists[Measure.Support]) m else m += Measure.Support(0),
      m => if (m.exists[Measure.HeadSize]) m else m += Measure.HeadSize(0),
      m => if (m.exists[Measure.HeadCoverage]) m else m += Measure.HeadCoverage(0.0)
    ))(TypedKeyMap(measures))
    Simple(head, body)(measuresMap)
  }

  implicit def apply(rule: Rule): FinalRule = rule match {
    case x: FinalRule => x
    case _ => Simple(rule.head, rule.body)(TypedKeyMap(
      Measure.Support(rule.support),
      Measure.HeadSize(rule.headSize),
      Measure.HeadCoverage(rule.headCoverage)
    ))
  }

  implicit val ruleOrdering: Ordering[Rule] = Ordering.by[Rule, TypedKeyMap.Immutable[Measure]](_.measures)

  implicit val ruleSimpleOrdering: Ordering[FinalRule] = Ordering.by[FinalRule, Rule](_.asInstanceOf[Rule])

  implicit val ruleSimilarityCounting: SimilarityCounting[Rule] = AtomsSimilarityCounting /*(0.5 * AtomsSimilarityCounting) ~
    (0.1 * LengthSimilarityCounting) ~
    (0.15 * SupportSimilarityCounting) ~
    (0.05 * ConfidenceSimilarityCounting) ~
    (0.15 * PcaConfidenceSimilarityCounting) ~
    (0.05 * LiftSimilarityCounting)*/

  implicit def ruleStringifier(implicit ruleSimpleStringifier: Stringifier[FinalRule]): Stringifier[Rule] = (v: Rule) => ruleSimpleStringifier.toStringValue(v match {
    case x: Simple => x
    case x: ExpandingRule => Simple(x.head, x.body)(TypedKeyMap())
    case x => Simple(x.head, x.body)(x.measures)
  })

  implicit def ruleSimpleStringifier(implicit resolvedRuleStringifier: Stringifier[ResolvedRule], mapper: TripleItemIndex): Stringifier[FinalRule] = (v: FinalRule) => resolvedRuleStringifier.toStringValue(v)

}