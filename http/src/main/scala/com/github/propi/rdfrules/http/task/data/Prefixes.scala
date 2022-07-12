package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, Prefix}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Prefixes extends Task[Dataset, ForEach[Prefix]] {
  val companion: TaskDefinition = Prefixes

  def execute(input: Dataset): ForEach[Prefix] = input.userDefinedPrefixes.toSeq
}

object Prefixes extends TaskDefinition {
  val name: String = "Prefixes"
}