package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern(antecedent: RulePattern.Constraint, consequent: RulePattern.Constraint)

object RulePattern {

  trait Constraint

  object Constraint {

    object * extends Constraint

    case class Atom(subjects: Set[String], predicates: Set[String], objects: Set[String]) extends Constraint

    case class And(atoms: List[Atom]) extends Constraint

  }

}
