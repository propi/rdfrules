package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class ToDatasetWithIntervals()(implicit debugger: Debugger) extends Task[Ruleset, Dataset] {
  val companion: TaskDefinition = ToDatasetWithIntervals

  def execute(input: Ruleset): Dataset = input.toDatasetWithIntervals

}

object ToDatasetWithIntervals extends TaskDefinition {
  val name: String = "ToDatasetWithIntervals"
}