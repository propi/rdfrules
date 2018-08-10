package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Slice(from: Int, until: Int) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Slice

  def execute(input: Dataset): Dataset = input.slice(from, until)
}

object Slice extends TaskDefinition {
  val name: String = "SliceQuads"
}