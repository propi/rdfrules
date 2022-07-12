package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class GetQuads extends Task[Dataset, QuadTraversableView] {
  val companion: TaskDefinition = GetQuads

  def execute(input: Dataset): QuadTraversableView = input.take(10000).cache.quads
}

object GetQuads extends TaskDefinition {
  val name: String = "GetQuads"
}