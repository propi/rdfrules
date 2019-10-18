package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadModel(path: String,  format: Option[Either[Int, RulesetSource]]) extends Task[Task.NoInput.type, Model] {
  val companion: TaskDefinition = LoadModel

  def execute(input: Task.NoInput.type): Model = format match {
    case Some(Left(1)) => Model.fromCache(Workspace.path(path))
    case Some(Right(RulesetSource.Json)) =>
    case _ => throw ValidationException("InvalidRulesetFormat", "Invalid rules format.")
  }Model.fromCache(input, Workspace.path(path))
}

object LoadModel extends TaskDefinition {
  val name: String = "LoadModel"
}