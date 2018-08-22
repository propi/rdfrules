package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, Quad}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GetQuads extends Task[Dataset, Traversable[Quad]] {
  val companion: TaskDefinition = GetQuads

  def execute(input: Dataset): Traversable[Quad] = input.take(10000).cache.quads
}

object GetQuads extends TaskDefinition {
  val name: String = "GetQuads"
}