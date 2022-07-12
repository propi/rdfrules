package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.{CoveredPaths, ResolvedRule, Ruleset}

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Instantiate(rule: Option[ResolvedRule], part: CoveredPaths.Part, allowDuplicateAtoms: Boolean) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = Instantiate

  def execute(input: Ruleset): Ruleset = {
    val filteredRuleset = rule match {
      case Some(rule) =>
        val ruleParts = rule.body.toSet -> rule.head
        input.filterResolved { x =>
          val ruleParts2 = x.body.toSet -> x.head
          ruleParts2 == ruleParts
        }
      case None => input
    }
    filteredRuleset.instantiate(part, allowDuplicateAtoms).map(_.paths).reduceOption(_ + _).getOrElse(Ruleset(input.index, Nil))
  }
}

object Instantiate extends TaskDefinition {
  val name: String = "Instantiate"
}