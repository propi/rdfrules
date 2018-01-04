package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
sealed trait RuleConstraint

object RuleConstraint {

  case class OnlyPredicates(predicates: Set[Int]) extends RuleConstraint

  case class WithoutPredicates(predicates: Set[Int]) extends RuleConstraint

  case class WithInstances(onlyObjects: Boolean) extends RuleConstraint

  case object WithoutDuplicitPredicates extends RuleConstraint

}
