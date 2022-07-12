package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Properties.PropertyStats
import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Properties extends Task[Dataset, Seq[(TripleItem.Uri, PropertyStats)]] {
  val companion: TaskDefinition = Properties

  def execute(input: Dataset): Seq[(TripleItem.Uri, PropertyStats)] = input.properties().iterator.toSeq.sortBy(x => x._2.sum)(implicitly[Ordering[Int]].reverse).take(10000)
}

object Properties extends TaskDefinition {
  val name: String = "Properties"
}