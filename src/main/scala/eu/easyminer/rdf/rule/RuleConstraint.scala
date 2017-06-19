package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
sealed trait RuleConstraint

object RuleConstraint {

  case class OnlyPredicates(predicates: Set[String]) extends RuleConstraint

  object WithInstances extends RuleConstraint

}
