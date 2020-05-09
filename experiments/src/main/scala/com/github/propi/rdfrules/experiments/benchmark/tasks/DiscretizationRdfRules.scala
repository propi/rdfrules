package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.experiments.IndexOps._
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, Task, TaskPostProcessor, TaskPreProcessor}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class DiscretizationRdfRules(val name: String, override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage)
                            (implicit val debugger: Debugger) extends Task[Index, Index, Index, Index] with TaskPreProcessor[Index, Index] with TaskPostProcessor[Index, Index] with DefaultMiningSettings {

  protected def postProcess(result: Index): Index = result

  protected def preProcess(input: Index): Index = input

  protected def taskBody(input: Index): Index = {
    val minSupport = input.useRichOps(_.getMinSupport(Threshold.MinHeadSize(100), Threshold.MinHeadCoverage(minHeadCoverage)))
    val predicates = input.useRichOps(_.getNumericPredicates(Iterator.empty, minSupport).toList)
    val trees = input.useRichOps(_.getDiscretizedTrees(predicates.iterator.map(_._1), minSupport, 2).toList)
    input.useRichOps(x => trees.foreach(y => x.addDiscretizedTreeToIndex(y._1.asInstanceOf[TripleItem.Uri], y._2)))
    input.tripleMap(_.reset())
    input
  }

}