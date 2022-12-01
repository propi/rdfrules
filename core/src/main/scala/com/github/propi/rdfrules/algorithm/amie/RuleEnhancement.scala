package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.{Atom, ExpandingRule, TripleItemPosition}

trait RuleEnhancement {

  val settings: AmieSettings
  val rule: ExpandingRule

  import settings._

  protected lazy val minCurrentSupport: Double = minComputedSupport(rule)

  /**
    * Next possible dangling variable for this rule
    */
  lazy val dangling: Atom.Variable = rule.maxVariable.++

  /**
    * Map of all rule predicates. Each predicate has subject and object variables.
    * For each this variable in a particular position it has list of other items (in subject or object position).
    * List of other items may contain both variables and constants.
    * Ex: "predicate 1" -> ( "subject variable a" -> List(object b, object B1, object B2) )
    */
  protected lazy val rulePredicates: collection.Map[Int, collection.Map[TripleItemPosition[Atom.Item], collection.Seq[Atom.Item]]] = {
    val map = collection.mutable.HashMap.empty[Int, collection.mutable.HashMap[TripleItemPosition[Atom.Item], collection.mutable.ListBuffer[Atom.Item]]]
    for (atom <- Iterator(rule.head) ++ rule.body.iterator) {
      for ((position, item) <- Iterable[(TripleItemPosition[Atom.Item], Atom.Item)](atom.subjectPosition -> atom.`object`, atom.objectPosition -> atom.subject) if position.item.isInstanceOf[Atom.Variable]) {
        map
          .getOrElseUpdate(atom.predicate, collection.mutable.HashMap.empty)
          .getOrElseUpdate(position, collection.mutable.ListBuffer.empty)
          .append(item)
      }
    }
    map
  }

  /**
    * if minAtomSize is lower than 0 then the atom size must be greater than or equal to minCurrentSupport
    */
  protected lazy val testAtomSize: Option[Int => Boolean] = {
    if (minAtomSize == 0) {
      None
    } else if (minAtomSize < 0) {
      Some((atomSize: Int) => atomSize >= minCurrentSupport)
    } else {
      Some((atomSize: Int) => atomSize >= minAtomSize)
    }
  }

}
