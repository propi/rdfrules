package com.github.propi.rdfrules.data.ops

import java.io.File

import com.github.propi.rdfrules.data.{DiscretizationTask, Quad, TripleItem}
import eu.easyminer.discretization.algorithm.Discretization
import eu.easyminer.discretization.impl.Interval
import eu.easyminer.discretization.impl.sorting.{ReversableSortedTraversable, SortedInMemoryNumericTraversable, SortedPersistentNumericTraversable}

/**
  * Created by Vaclav Zeman on 26. 2. 2018.
  */
trait Discretizable[Coll] extends QuadsOps[Coll] {

  /**
    * Use a discretization task to make intervals from numerical objects.
    * This is the strict action for filtered quads depending on the buffer size defined on the discretization task (Equifrequency and Equisize).
    * This is the streaming action for Equidistance
    *
    * @param task discretization task
    * @param f    filter for selection of quads to discretize
    * @return intervals
    */
  def discretizeAndGetIntervals(task: DiscretizationTask)(f: Quad => Boolean): IndexedSeq[Interval] = {
    def makeSortedTraversable(col: Traversable[Double], mode: DiscretizationTask.Mode)(f: ReversableSortedTraversable[Double] => IndexedSeq[Interval]): IndexedSeq[Interval] = mode match {
      case DiscretizationTask.Mode.InMemory => f(SortedInMemoryNumericTraversable(col, task.getBufferSize))
      case DiscretizationTask.Mode.External => SortedPersistentNumericTraversable(col, Discretizable.tempDirectory, task.getBufferSize)(f)
    }

    val dis = Discretization[Double](task)
    val col = quads.filter(f).map(_.triple.`object`).collect {
      case TripleItem.NumberDouble(x) => x
    }
    task match {
      case _: DiscretizationTask.Equidistance => dis.discretize(col)
      case x: DiscretizationTask.Equifrequency => makeSortedTraversable(col, x.mode)(rst => dis.discretize(rst))
      case x: DiscretizationTask.Equisize => makeSortedTraversable(col, x.mode)(rst => dis.discretize(rst))
      case _: DiscretizationTask.EquisizeTree => makeSortedTraversable(col, DiscretizationTask.Mode.InMemory)(rst => dis.discretize(rst))
    }
  }

  /**
    * Transform triples by a discretization task.
    * Partially streaming transformation depending on the discretization task.
    *
    * @param task discretization task
    * @param f    filter for selection of quads to discretize
    * @return
    */
  def discretize(task: DiscretizationTask)(f: Quad => Boolean): Coll = {
    lazy val intervals = discretizeAndGetIntervals(task)(f)
    val col = quads.map { quad =>
      if (f(quad)) {
        quad.triple.`object` match {
          case TripleItem.NumberDouble(x) => intervals
            .find(_.isInInterval(x))
            .map(x => quad.copy(triple = quad.triple.copy(`object` = TripleItem.Interval(x))))
            .getOrElse(quad)
          case _ => quad
        }
      } else {
        quad
      }
    }
    transformQuads(col)
  }

}

object Discretizable {

  private lazy val tempDirectory = {
    val dir = new File("temp")
    if (!dir.isDirectory) dir.mkdir()
    dir
  }

}