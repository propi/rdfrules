package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Index(implicit debugger: Debugger) extends Task[Dataset, index.Index] {
  val companion: TaskDefinition = Index

  def execute(input: Dataset): index.Index = input.index()
}

object Index extends TaskDefinition {
  val name: String = "Index"
}