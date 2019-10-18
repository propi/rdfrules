package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.ruleset.ResolvedRule

/**
  * Created by Vaclav Zeman on 26. 4. 2018.
  */
trait ResolvedRulePatternMatcher[T] {
  def matchPattern(x: T, pattern: RulePattern): Boolean
}

object ResolvedRulePatternMatcher {

  private def matchAntecedent(pattern: IndexedSeq[AtomPattern], body: IndexedSeq[ResolvedRule.Atom])(implicit atomMatcher: ResolvedAtomPatternMatcher[ResolvedRule.Atom]): Boolean = {
    val bodySet = collection.mutable.Set(body: _*)
    pattern.forall { atomPattern =>
      bodySet.find(atomMatcher.matchPattern(_, atomPattern)) match {
        case Some(x) =>
          bodySet -= x
          true
        case None => false
      }
    }
  }

  implicit def resolvedRulePatternMatcher(implicit atomMatcher: ResolvedAtomPatternMatcher[ResolvedRule.Atom]): ResolvedRulePatternMatcher[ResolvedRule] = (x: ResolvedRule, pattern: RulePattern) => {
    pattern.consequent.forall(atomMatcher.matchPattern(x.head, _)) &&
      (pattern.exact && pattern.antecedent.size == x.body.size || !pattern.exact && pattern.antecedent.size <= x.body.size) &&
      matchAntecedent(pattern.antecedent, x.body)
  }

}