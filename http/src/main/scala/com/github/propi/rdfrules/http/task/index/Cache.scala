package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String) extends Task[Index, Index] {
  val companion: TaskDefinition = Cache

  def execute(input: Index): Index = input.cache(Workspace.path(path))
}

object Cache extends TaskDefinition {
  val name: String = "CacheIndex"
}