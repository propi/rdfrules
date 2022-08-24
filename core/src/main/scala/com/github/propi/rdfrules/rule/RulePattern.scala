package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern private(body: IndexedSeq[AtomPattern], head: Option[AtomPattern], exact: Boolean, orderless: Boolean) {

  /**
    * Add an atom pattern as a body pattern. It adds the pattern into the left side.
    *
    * @param atom atom pattern
    * @return a new rule pattern with added atom body pattern
    */
  def &:(atom: AtomPattern): RulePattern = this.copy(body = atom +: body)

  /**
    * Create a new rule pattern with changed exact property.
    *
    * @param exact true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return a new rule pattern
    */
  def withExact(exact: Boolean = true): RulePattern = this.copy(exact = exact)

  /**
    * The pattern of the body is not evaluated gradually. The ordering of atoms does not matter.
    *
    * @param orderless true = any atom in the body can be placed in any position of the body of the rule,
    *                  false = patterns are gradually matching by rule refining.
    * @return a new rule pattern
    */
  def withOrderless(orderless: Boolean = true): RulePattern = this.copy(orderless = orderless)

  /**
    * Create mapped rule pattern with mapped triple items to hash numbers
    *
    * @param mapper triple item mapper
    * @return a mapped rule pattern
    */
  def mapped(implicit mapper: TripleItemIndex): RulePattern.Mapped = RulePattern.Mapped(body.map(_.mapped), head.map(_.mapped), exact, orderless)

  def matchWith[T](x: T)(implicit matcher: PatternMatcher[T, RulePattern]): Boolean = matcher.matchPattern(x, this)(Aliases.empty).isDefined
}

object RulePattern {

  case class Mapped(body: IndexedSeq[AtomPattern.Mapped], head: Option[AtomPattern.Mapped], exact: Boolean, orderless: Boolean) {
    def matchWith[T](x: T)(implicit matcher: PatternMatcher[T, Mapped]): Boolean = matcher.matchPattern(x, this)(Aliases.empty).isDefined
  }

  /* {
      def isMatching[T](x: T)(implicit rulePatternMatcher: MappedRulePatternMatcher[T]): Boolean = rulePatternMatcher.matchPattern(x, this)
    }*/

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern may be exect or partial.
    *
    * @param head  consequent pattern, None = no pattern for consequent.
    * @param exact true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return rule pattern
    */
  def apply(head: Option[AtomPattern], exact: Boolean, orderless: Boolean): RulePattern = RulePattern(Vector.empty, head, exact, orderless)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern may be exect or partial.
    *
    * @param head  consequent pattern
    * @param exact true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return rule pattern
    */
  def apply(head: AtomPattern, exact: Boolean, orderless: Boolean): RulePattern = apply(Some(head), exact, orderless)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern is not exact.
    *
    * @param head consequent pattern, None = no pattern for consequent.
    * @return rule pattern
    */
  implicit def apply(head: Option[AtomPattern]): RulePattern = apply(head, false, false)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern is not exact.
    *
    * @param head consequent pattern
    * @return rule pattern
    */
  implicit def apply(head: AtomPattern): RulePattern = apply(head, false, false)

  /**
    * Create a rule pattern with none pattern as the consequent.
    * The pattern is not exact.
    *
    * @return rule pattern
    */
  def apply(): RulePattern = apply(None, false, false)

}