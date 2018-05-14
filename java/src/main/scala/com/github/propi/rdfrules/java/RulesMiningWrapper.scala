package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.rule.RuleConstraint

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
class RulesMiningWrapper(rulesMining: com.github.propi.rdfrules.algorithm.RulesMining) {

  def withOnlyPredicates(predicates: java.util.Set[Integer]): com.github.propi.rdfrules.algorithm.RulesMining = rulesMining.addConstraint(RuleConstraint.OnlyPredicates(predicates.iterator().asScala.map(_.intValue()).toSet))

  def withoutPredicates(predicates: java.util.Set[Integer]): com.github.propi.rdfrules.algorithm.RulesMining = rulesMining.addConstraint(RuleConstraint.WithoutPredicates(predicates.iterator().asScala.map(_.intValue()).toSet))

}
