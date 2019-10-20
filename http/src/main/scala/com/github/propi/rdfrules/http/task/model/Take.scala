package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Take(n: Int) extends Task[Model, Model] {
  val companion: TaskDefinition = Take

  def execute(input: Model): Model = input.take(n)
}

object Take extends TaskDefinition {
  val name: String = "TakeRules"
}