package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{QuadMatcher, Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class FilterQuads(quadMatchers: Seq[(QuadMatcher, Boolean)]) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = FilterQuads

  def execute(input: Dataset): Dataset = input.filter { quad =>
    quadMatchers.exists { case (quadMatcher, inverse) =>
      quadMatcher.matchAll(quad).matched ^ inverse
    }
  }
}

object FilterQuads extends TaskDefinition {
  val name: String = "FilterQuads"
}

