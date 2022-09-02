package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.{Index, PropertyCardinalities, TripleItemIndex}

class PropertiesCardinalities(filter: Set[TripleItem.Uri]) extends Task[Index, Seq[PropertyCardinalities.Resolved]] {
  val companion: TaskDefinition = PropertiesCardinalities

  def execute(input: Index): Seq[PropertyCardinalities.Resolved] = {
    implicit val tim: TripleItemIndex = input.tripleItemMap
    lazy val mappedFilter = filter.flatMap(tim.getIndexOpt)
    val res = if (filter.isEmpty && mappedFilter.isEmpty) {
      input.properties
    } else {
      input.properties(mappedFilter)
    }
    res.take(10000).map(_.resolved).toSeq
  }
}

object PropertiesCardinalities extends TaskDefinition {
  val name: String = "PropertiesCardinalities"
}