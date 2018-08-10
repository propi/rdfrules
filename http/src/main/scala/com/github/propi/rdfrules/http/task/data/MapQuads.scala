package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{QuadMapper, QuadMatcher, Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class MapQuads(quadMatcher: QuadMatcher, quadMapper: QuadMapper, inverse: Boolean) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = MapQuads

  def execute(input: Dataset): Dataset = input.map { quad =>
    val capturedQuadItems = quadMatcher.matchAll(quad)
    if (inverse && !capturedQuadItems.matched) {
      quadMapper.map(quad, QuadMatcher(None, None, None, None).matchAll(quad))
    } else if (!inverse && capturedQuadItems.matched) {
      quadMapper.map(quad, capturedQuadItems)
    } else {
      quad
    }
  }
}

object MapQuads extends TaskDefinition {
  val name: String = "MapQuads"
}