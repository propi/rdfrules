package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.ruleset.RulesetSource
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadModel(path: String, format: Option[Option[RulesetSource]])(implicit debugger: Debugger) extends Task[Task.NoInput.type, Model] {
  val companion: TaskDefinition = LoadModel

  def execute(input: Task.NoInput.type): Model = {
    val model = format match {
      case Some(Some(source)) => Model(Workspace.path(path))(source)
      case Some(None) => Model.fromCache(Workspace.path(path))
      case None => Model(Workspace.path(path))
    }
    model.withDebugger
  }
}

object LoadModel extends TaskDefinition {
  val name: String = "LoadModel"
}