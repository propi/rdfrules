package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.ExpandingRule
import com.github.propi.rdfrules.rule.ExpandingRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.utils.MutableRanges

object RuleHeadsValidation {

  implicit class PimpedExpandingRule(val rule: ExpandingRule) extends AnyVal {
    def headValidator[T](f: HeadValidator => T): T = {
      val validators = (Iterator(rule) ++ Iterator.iterate(rule.parent)(_.flatMap(_.parent)).takeWhile(_.isDefined).map(_.get)).flatMap(_.supportedRanges).map(_.validator).toList
      f(new HeadValidator(validators))
    }

    def withSupportedRanges(ranges: MutableRanges, parent: ExpandingRule): ExpandingRule = rule match {
      case x: ClosedRule => ClosedRule(x.body, x.head, x.support, x.headSize, x.variables, x.maxVariable, ranges, parent)
      case x: DanglingRule => DanglingRule(x.body, x.head, x.support, x.headSize, x.variables, x.maxVariable, ranges, parent)
    }
  }

  class HeadValidator private[RuleHeadsValidation](validators: Iterable[MutableRanges.Validator]) {
    def isValid(x: Int): Boolean = validators.exists(_.isInRange(x))
  }

}