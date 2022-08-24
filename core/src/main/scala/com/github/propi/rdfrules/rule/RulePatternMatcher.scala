package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.rule.PatternMatcher.Aliases

object RulePatternMatcher {

  private def matchAntecedentOrderless[A, AP](body: Set[A], pattern: Seq[AP])(implicit atomMatcher: PatternMatcher[A, AP], aliases: Aliases): Option[Aliases] = {
    if (pattern.isEmpty) {
      Some(aliases)
    } else {
      val head = pattern.head
      val tail = pattern.tail
      body.view.flatMap(x => atomMatcher.matchPattern(x, head).map(x -> _)).flatMap { case (atom, aliases) =>
        matchAntecedentOrderless(body - atom, tail)(atomMatcher, aliases)
      }.headOption
    }
  }

  private def matchAntecedentGradually[A, AP](body: IndexedSeq[A], pattern: IndexedSeq[AP])(implicit atomMatcher: PatternMatcher[A, AP], aliases: Aliases): Option[Aliases] = {
    body.reverseIterator.zip(pattern.reverseIterator).foldLeft(Option(aliases))((res, x) => res.flatMap { implicit aliases =>
      atomMatcher.matchPattern(x._1, x._2)
    })
  }

  implicit def mappedRulePatternMatcher(implicit atomMatcher: MappedAtomPatternMatcher[Atom]): PatternMatcher[Rule, RulePattern.Mapped] = new PatternMatcher[Rule, RulePattern.Mapped] {
    def matchPattern(x: Rule, pattern: RulePattern.Mapped)(implicit aliases: PatternMatcher.Aliases): Option[PatternMatcher.Aliases] = {
      (pattern.head match {
        case Some(head) => atomMatcher.matchPattern(x.head, head)
        case None => Some(aliases)
      }).filter(_ => pattern.exact && pattern.body.size == x.body.size || !pattern.exact && pattern.body.size <= x.body.size).flatMap { implicit aliases =>
        if (pattern.orderless) matchAntecedentOrderless(x.body.toSet, pattern.body) else matchAntecedentGradually(x.body, pattern.body)
      }
    }
  }

  implicit def resolvedRulePatternMatcher(implicit atomMatcher: ResolvedAtomPatternMatcher[ResolvedAtom]): PatternMatcher[ResolvedRule, RulePattern] = new PatternMatcher[ResolvedRule, RulePattern] {
    def matchPattern(x: ResolvedRule, pattern: RulePattern)(implicit aliases: Aliases): Option[Aliases] = {
      (pattern.head match {
        case Some(head) => atomMatcher.matchPattern(x.head, head)
        case None => Some(aliases)
      }).filter(_ => pattern.exact && pattern.body.size == x.body.size || !pattern.exact && pattern.body.size <= x.body.size).flatMap { implicit aliases =>
        if (pattern.orderless) matchAntecedentOrderless(x.body.toSet, pattern.body) else matchAntecedentGradually(x.body, pattern.body)
      }
    }
  }

}