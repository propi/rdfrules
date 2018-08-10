package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask}
import com.github.propi.rdfrules.http.task.{QuadMatcher, Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Discretize(quadMatcher: QuadMatcher, inverse: Boolean, discretizationTask: DiscretizationTask) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Discretize

  def execute(input: Dataset): Dataset = input.discretize(discretizationTask)(quad => quadMatcher.matchAll(quad).matched ^ inverse)
}

object Discretize extends TaskDefinition {
  val name: String = "Discretize"
}