package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemHashIndex
import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
sealed trait RuleConstraint extends Value {
  def companion: Key[RuleConstraint]
}

object RuleConstraint {

  case class OnlyPredicates(predicates: Set[TripleItem.Uri]) extends RuleConstraint {
    def companion: OnlyPredicates.type = OnlyPredicates

    def mapped(implicit mapper: TripleItemHashIndex): Set[Int] = predicates.map(x => mapper.getIndex(x.resolved))
  }

  implicit object OnlyPredicates extends Key[OnlyPredicates] {
    def apply(predicates: TripleItem.Uri*): OnlyPredicates = new OnlyPredicates(predicates.toSet)
  }

  case class WithoutPredicates(predicates: Set[TripleItem.Uri]) extends RuleConstraint {
    def companion: WithoutPredicates.type = WithoutPredicates

    def mapped(implicit mapper: TripleItemHashIndex): Set[Int] = predicates.map(x => mapper.getIndex(x.resolved))
  }

  implicit object WithoutPredicates extends Key[WithoutPredicates] {
    def apply(predicates: TripleItem.Uri*): WithoutPredicates = new WithoutPredicates(predicates.toSet)
  }

  case class WithInstances(onlyObjects: Boolean) extends RuleConstraint {
    def companion: WithInstances.type = WithInstances
  }

  implicit object WithInstances extends Key[WithInstances]

  case class WithoutDuplicitPredicates() extends RuleConstraint {
    def companion: WithoutDuplicitPredicates.type = WithoutDuplicitPredicates
  }

  implicit object WithoutDuplicitPredicates extends Key[WithoutDuplicitPredicates]

  implicit def ruleConstraintToKeyValue(ruleConstraint: RuleConstraint): (Key[RuleConstraint], RuleConstraint) = ruleConstraint.companion -> ruleConstraint

}