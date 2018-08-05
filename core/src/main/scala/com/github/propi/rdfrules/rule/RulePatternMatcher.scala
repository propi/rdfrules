package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 26. 4. 2018.
  */
trait RulePatternMatcher[T] {
  def matchPattern(x: T, pattern: RulePattern.Mapped): Boolean
}

object RulePatternMatcher {

  implicit def rulePatternMatcher(implicit atomMatcher: AtomPatternMatcher[Atom]): RulePatternMatcher[Rule] = (x: Rule, pattern: RulePattern.Mapped) => {
    pattern.consequent.forall(atomMatcher.matchPattern(x.head, _)) &&
      (pattern.exact && pattern.antecedent.size == x.body.size || !pattern.exact && pattern.antecedent.size <= x.body.size) &&
      pattern.antecedent.view.zip(x.body.takeRight(pattern.antecedent.size)).forall(x => atomMatcher.matchPattern(x._2, x._1))
  }

}