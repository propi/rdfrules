package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.ruleset.ResolvedRule

object RulePatternMatcher {

  private def matchAntecedentOrderless[A, AP](body: Set[A], pattern: Seq[AP])(implicit atomMatcher: PatternMatcher[A, AP]): Boolean = {
    if (pattern.isEmpty) {
      true
    } else {
      val head = pattern.head
      val tail = pattern.tail
      body.iterator.filter(atomMatcher.matchPattern(_, head)).exists(atom => matchAntecedentOrderless(body - atom, tail))
    }
  }

  private def matchAntecedentGradually[A, AP](body: IndexedSeq[A], pattern: IndexedSeq[AP])(implicit atomMatcher: PatternMatcher[A, AP]): Boolean = {
    body.reverseIterator.zip(pattern.reverseIterator).forall(x => atomMatcher.matchPattern(x._1, x._2))
  }

  implicit def mappedRulePatternMatcher(implicit atomMatcher: MappedAtomPatternMatcher[Atom]): PatternMatcher[Rule, RulePattern.Mapped] = (x: Rule, pattern: RulePattern.Mapped) => {
    pattern.head.forall(atomMatcher.matchPattern(x.head, _)) &&
      (pattern.exact && pattern.body.size == x.body.size || !pattern.exact && pattern.body.size <= x.body.size) &&
      (if (pattern.orderless) matchAntecedentOrderless(x.body.toSet, pattern.body) else matchAntecedentGradually(x.body, pattern.body))
  }

  implicit def resolvedRulePatternMatcher(implicit atomMatcher: ResolvedAtomPatternMatcher[ResolvedRule.Atom]): PatternMatcher[ResolvedRule, RulePattern] = (x: ResolvedRule, pattern: RulePattern) => {
    pattern.head.forall(atomMatcher.matchPattern(x.head, _)) &&
      (pattern.exact && pattern.body.size == x.body.size || !pattern.exact && pattern.body.size <= x.body.size) &&
      (if (pattern.orderless) matchAntecedentOrderless(x.body.toSet, pattern.body) else matchAntecedentGradually(x.body, pattern.body))
  }

}