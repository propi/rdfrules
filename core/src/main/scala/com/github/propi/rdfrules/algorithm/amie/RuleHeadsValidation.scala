package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.ExpandingRule
import com.github.propi.rdfrules.rule.ExpandingRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.utils.MutableRanges

object RuleHeadsValidation {

  implicit class PimpedExpandingRule(val rule: ExpandingRule) extends AnyVal {
    def headValidator[T](f: MutableRanges.Validator => T): T = {
      f(rule.supportedRanges.map(_.validator).getOrElse(MutableRanges.AlwaysTrueValidator))
    }

    def withSupportedRanges(ranges: MutableRanges): ExpandingRule = rule match {
      case x: ClosedRule => ClosedRule(x.body, x.head, x.support, x.headSize, x.variables, ranges)
      case x: DanglingRule => DanglingRule(x.body, x.head, x.support, x.headSize, x.variables, ranges)
    }
  }

}