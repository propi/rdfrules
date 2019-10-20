package com.github.propi.rdfrules.http.task.model

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.model.Model

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Sorted extends Task[Model, Model] {
  val companion: TaskDefinition = Sorted

  def execute(input: Model): Model = input.sorted
}

object Sorted extends TaskDefinition {
  val name: String = "Sorted"
}