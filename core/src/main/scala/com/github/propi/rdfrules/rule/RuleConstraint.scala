package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsForPredicates.ConstantsForPredicatePosition
import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
sealed trait RuleConstraint extends Value {
  def companion: Key[RuleConstraint]
}

object RuleConstraint {

  trait Filter extends RuleConstraint {
    def mapped(implicit mapper: TripleItemIndex): MappedFilter
  }

  trait MappedFilter {
    def test(newAtom: Atom, rule: Option[Rule]): Boolean
  }

  case class OnlyPredicates(predicates: Set[TripleItem.Uri]) extends RuleConstraint {
    def companion: OnlyPredicates.type = OnlyPredicates

    def mapped(implicit mapper: TripleItemIndex): Set[Int] = predicates.map(x => mapper.getIndex(x))
  }

  implicit object OnlyPredicates extends Key[OnlyPredicates] {
    def apply(predicates: TripleItem.Uri*): OnlyPredicates = new OnlyPredicates(predicates.toSet)
  }

  case class WithoutPredicates(predicates: Set[TripleItem.Uri]) extends RuleConstraint {
    def companion: WithoutPredicates.type = WithoutPredicates

    def mapped(implicit mapper: TripleItemIndex): Set[Int] = predicates.map(x => mapper.getIndex(x))
  }

  implicit object WithoutPredicates extends Key[WithoutPredicates] {
    def apply(predicates: TripleItem.Uri*): WithoutPredicates = new WithoutPredicates(predicates.toSet)
  }

  case class ConstantsForPredicates(predicates: Map[TripleItem.Uri, ConstantsForPredicatePosition]) extends RuleConstraint {
    def companion: ConstantsForPredicates.type = ConstantsForPredicates

    def mapped(implicit mapper: TripleItemIndex): Map[Int, ConstantsForPredicatePosition] = predicates.iterator.map(x => mapper.getIndex(x._1) -> x._2).toMap
  }

  implicit object ConstantsForPredicates extends Key[ConstantsForPredicates] {

    sealed trait ConstantsForPredicatePosition

    object ConstantsForPredicatePosition {
      case object Subject extends ConstantsForPredicatePosition

      case object Object extends ConstantsForPredicatePosition

      case object LowerCardinalitySide extends ConstantsForPredicatePosition

      case object Both extends ConstantsForPredicatePosition
    }

  }

  case class ConstantsAtPosition(position: ConstantsPosition) extends RuleConstraint {
    def companion: ConstantsAtPosition.type = ConstantsAtPosition
  }

  implicit object ConstantsAtPosition extends Key[ConstantsAtPosition] {

    sealed trait ConstantsPosition

    object ConstantsPosition {

      case object Subject extends ConstantsPosition

      case object Object extends ConstantsPosition

      case class LowerCardinalitySide(headOnly: Boolean = false) extends ConstantsPosition

      case object Nowhere extends ConstantsPosition

    }

  }

  case class InjectiveMapping() extends RuleConstraint {
    def companion: InjectiveMapping.type = InjectiveMapping
  }

  implicit object InjectiveMapping extends Key[InjectiveMapping]

  case class WithoutDuplicatePredicates() extends RuleConstraint {
    def companion: WithoutDuplicatePredicates.type = WithoutDuplicatePredicates
  }

  implicit object WithoutDuplicatePredicates extends Key[WithoutDuplicatePredicates]

  implicit def ruleConstraintToKeyValue(ruleConstraint: RuleConstraint): (Key[RuleConstraint], RuleConstraint) = ruleConstraint.companion -> ruleConstraint

}