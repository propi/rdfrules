package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadIndex(path: String, partially: Boolean)(implicit debugger: Debugger) extends Task[Task.NoInput.type, Index] {
  val companion: TaskDefinition = LoadIndex

  def execute(input: Task.NoInput.type): Index = Index.fromCache(Workspace.path(path), partially)
}

object LoadIndex extends TaskDefinition {
  val name: String = "LoadIndex"
}