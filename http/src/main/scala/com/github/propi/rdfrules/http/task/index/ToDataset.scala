package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.Index

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class ToDataset extends Task[Index, Dataset] {
  val companion: TaskDefinition = ToDataset

  def execute(input: Index): Dataset = input.toDataset
}

object ToDataset extends TaskDefinition {
  val name: String = "IndexToDataset"
}