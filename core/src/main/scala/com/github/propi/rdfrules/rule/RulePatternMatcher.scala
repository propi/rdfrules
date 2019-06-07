package com.github.propi.rdfrules.rule

/**
  * Created by Vaclav Zeman on 26. 4. 2018.
  */
trait RulePatternMatcher[T] {
  def matchPattern(x: T, pattern: RulePattern.Mapped): Boolean
}

object RulePatternMatcher {

  private def matchAntecedent(pattern: IndexedSeq[AtomPattern.Mapped], body: IndexedSeq[Atom])(implicit atomMatcher: AtomPatternMatcher[Atom]): Boolean = {
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

  implicit def rulePatternMatcher(implicit atomMatcher: AtomPatternMatcher[Atom]): RulePatternMatcher[Rule] = (x: Rule, pattern: RulePattern.Mapped) => {
    pattern.consequent.forall(atomMatcher.matchPattern(x.head, _)) &&
      (pattern.exact && pattern.antecedent.size == x.body.size || !pattern.exact && pattern.antecedent.size <= x.body.size) &&
      matchAntecedent(pattern.antecedent, x.body)
  }

}