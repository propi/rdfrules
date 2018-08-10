package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Size extends Task[Dataset, Int] {
  val companion: TaskDefinition = Size

  def execute(input: Dataset): Int = input.size
}

object Size extends TaskDefinition {
  val name: String = "DatasetSize"
}