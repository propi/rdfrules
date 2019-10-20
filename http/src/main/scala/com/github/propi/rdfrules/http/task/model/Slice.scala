package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Slice(from: Int, until: Int) extends Task[Model, Model] {
  val companion: TaskDefinition = Slice

  def execute(input: Model): Model = input.slice(from, until)
}

object Slice extends TaskDefinition {
  val name: String = "SliceRules"
}