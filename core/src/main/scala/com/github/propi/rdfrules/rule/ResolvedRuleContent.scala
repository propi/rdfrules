package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.utils.Stringifier

import scala.language.implicitConversions

trait ResolvedRuleContent {
  val body: IndexedSeq[ResolvedAtom]
  val head: ResolvedAtom

  def ruleLength: Int = body.size + 1

  override def toString: String = Stringifier(this)
}

object ResolvedRuleContent {

  private case class BasicResolvedRuleContent(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom) extends ResolvedRuleContent

  def apply(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom): ResolvedRuleContent = BasicResolvedRuleContent(body, head)

  implicit def apply(ruleContent: RuleContent)(implicit mapper: TripleItemIndex): ResolvedRuleContent = BasicResolvedRuleContent(
    ruleContent.body.map(ResolvedAtom.apply),
    ruleContent.head
  )

  implicit val resolvedRuleContentStringifier: Stringifier[ResolvedRuleContent] = (v: ResolvedRuleContent) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head)

}