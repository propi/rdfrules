package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Drop(n: Int) extends Task[Model, Model] {
  val companion: TaskDefinition = Drop

  def execute(input: Model): Model = input.drop(n)
}

object Drop extends TaskDefinition {
  val name: String = "DropRules"
}