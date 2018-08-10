package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Cache(path: String) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Cache

  def execute(input: Dataset): Dataset = input.cache(path)
}

object Cache extends TaskDefinition {
  val name: String = "CacheDataset"
}