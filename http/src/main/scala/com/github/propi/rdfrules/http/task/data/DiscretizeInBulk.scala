package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask, TripleItem, TripleItemType}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.utils.Debugger
import eu.easyminer.discretization.impl.Interval

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class DiscretizeInBulk(predicates: Set[TripleItem.Uri], discretizationTask: DiscretizationTask)(implicit debugger: Debugger) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = DiscretizeInBulk

  def execute(input: Dataset): Dataset = {
    val cachedInput = input.cache.withDebugger()
    val predicatesToProcess = if (predicates.isEmpty) {
      cachedInput.properties().iterator.filter(_._2.iterator.exists(_._1 == TripleItemType.Number)).map(_._1).toSet
    } else predicates

    val intervals = debugger.debug("Bulk discretization", predicatesToProcess.size, true) { ad =>
      predicatesToProcess.foldLeft(Map.empty[TripleItem.Uri, IndexedSeq[Interval]])((intervals, predicate) =>
        ad.result()(intervals + (predicate -> cachedInput.discretizeAndGetIntervals(discretizationTask)(quad => quad.triple.predicate == predicate)))
      )
    }

    cachedInput.mapNumbersToIntervals(q => intervals.getOrElse(q.triple.predicate, IndexedSeq.empty))
  }
}

object DiscretizeInBulk extends TaskDefinition {
  val name: String = "DiscretizeInBulk"
}