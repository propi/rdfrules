package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.java.data.TripleItem
import com.github.propi.rdfrules.rule.RuleConstraint

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
class RulesMiningWrapper(rulesMining: com.github.propi.rdfrules.algorithm.RulesMining) {

  def withOnlyPredicates(predicates: java.util.Set[TripleItem.Uri]): com.github.propi.rdfrules.algorithm.RulesMining = rulesMining.addConstraint(RuleConstraint.OnlyPredicates(predicates.iterator().asScala.map(_.asScala()).toSet))

  def withoutPredicates(predicates: java.util.Set[TripleItem.Uri]): com.github.propi.rdfrules.algorithm.RulesMining = rulesMining.addConstraint(RuleConstraint.WithoutPredicates(predicates.iterator().asScala.map(_.asScala()).toSet))

}
