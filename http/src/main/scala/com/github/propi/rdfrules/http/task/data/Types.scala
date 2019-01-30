package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, TripleItem, TripleItemType}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

import scala.util.Try

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Types extends Task[Dataset, Seq[(TripleItem.Uri, collection.Map[TripleItemType, Int])]] {
  val companion: TaskDefinition = Types

  def execute(input: Dataset): Seq[(TripleItem.Uri, collection.Map[TripleItemType, Int])] = input.types().iterator.toSeq.sortBy(x => Try(x._2.valuesIterator.sum).getOrElse(0))(implicitly[Ordering[Int]].reverse).take(10000)
}

object Types extends TaskDefinition {
  val name: String = "Types"
}