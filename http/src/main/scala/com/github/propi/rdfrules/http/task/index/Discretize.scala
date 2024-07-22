package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.{AutoDiscretizationTask, Index}

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class Discretize(task: AutoDiscretizationTask) extends Task[Index, Dataset] {
  val companion: TaskDefinition = Discretize

  def execute(input: Index): Dataset = input.discretize(task)
}

object Discretize extends TaskDefinition {
  val name: String = "AutoDiscretization"
}