package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.ruleset.ResolvedRule

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GetRules extends Task[Model, Traversable[ResolvedRule]] {
  val companion: TaskDefinition = GetRules

  def execute(input: Model): Traversable[ResolvedRule] = input.take(10000).cache.rules
}

object GetRules extends TaskDefinition {
  val name: String = "GetRules"
}