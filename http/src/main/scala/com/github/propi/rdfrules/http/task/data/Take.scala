package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Take(n: Int) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Take

  def execute(input: Dataset): Dataset = input.take(n)
}

object Take extends TaskDefinition {
  val name: String = "TakeQuads"
}