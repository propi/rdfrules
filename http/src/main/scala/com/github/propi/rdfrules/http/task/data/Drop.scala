package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Drop(n: Int) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Drop

  def execute(input: Dataset): Dataset = input.drop(n)
}

object Drop extends TaskDefinition {
  val name: String = "DropQuads"
}