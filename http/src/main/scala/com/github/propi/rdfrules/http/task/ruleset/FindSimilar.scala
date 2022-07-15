package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class FindSimilar(resolvedRule: ResolvedRule, k: Int, dissimilar: Boolean)(implicit similarityCounting: SimilarityCounting[FinalRule]) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = FindSimilar

  def execute(input: Ruleset): Ruleset = if (dissimilar) input.findDissimilar(resolvedRule, k) else input.findSimilar(resolvedRule, k)
}

object FindSimilar extends TaskDefinition {
  val name: String = "FindSimilar"
}