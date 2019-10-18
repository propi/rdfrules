package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String) extends Task[Model, Model] {
  val companion: TaskDefinition = Cache

  def execute(input: Model): Model = input.cache(Workspace.path(path))
}

object Cache extends TaskDefinition {
  val name: String = "CacheRuleset"
}