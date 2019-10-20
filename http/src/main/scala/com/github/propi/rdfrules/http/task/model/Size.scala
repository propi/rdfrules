package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Size extends Task[Model, Int] {
  val companion: TaskDefinition = Size

  def execute(input: Model): Int = input.size
}

object Size extends TaskDefinition {
  val name: String = "RulesetSize"
}