package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.data.{DiscretizationTask, Quad, TripleItem}
import com.github.propi.rdfrules.utils.ForEach
import eu.easyminer.discretization.Consumer
import eu.easyminer.discretization.algorithm.Discretization
import eu.easyminer.discretization.impl.sorting.{ReversableSortedProducer, SortedInMemoryNumericProducer, SortedPersistentNumericProducer}
import eu.easyminer.discretization.impl.{Interval, Producer}

import java.io.File

/**
  * Created by Vaclav Zeman on 26. 2. 2018.
  */
trait Discretizable[Coll] extends QuadsOps[Coll] {

  private def toProducer[T](x: ForEach[T]): Producer[T] = (consumer: Consumer[T]) => x.foreach(consumer.consume)

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
    def makeSortedTraversable(col: ForEach[Double], mode: DiscretizationTask.Mode)(f: ReversableSortedProducer[Double] => IndexedSeq[Interval]): IndexedSeq[Interval] = mode match {
      case DiscretizationTask.Mode.InMemory => f(SortedInMemoryNumericProducer(toProducer(col), task.getBufferSize))
      case DiscretizationTask.Mode.External => SortedPersistentNumericProducer(toProducer(col), Discretizable.tempDirectory, task.getBufferSize)(f)
    }

    val dis = Discretization[Double](task)
    val col = quads.filter(f).map(_.triple.`object`).collect {
      case TripleItem.NumberDouble(x) => x
    }
    task match {
      case _: DiscretizationTask.Equidistance => dis.discretize(toProducer(col))
      case x: DiscretizationTask.Equifrequency => makeSortedTraversable(col, x.mode)(rst => dis.discretize(rst))
      case x: DiscretizationTask.Equisize => makeSortedTraversable(col, x.mode)(rst => dis.discretize(rst))
      case _: DiscretizationTask.EquisizeTree => makeSortedTraversable(col, DiscretizationTask.Mode.InMemory)(rst => dis.discretize(rst))
    }
  }

  def mapNumbersToIntervals(f: Quad => IndexedSeq[Interval]): Coll = transformQuads(quads.map { quad =>
    val intervals = f(quad)
    if (intervals.nonEmpty) {
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
  })

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
    mapNumbersToIntervals(q => if (f(q)) intervals else IndexedSeq.empty)
  }

}

object Discretizable {

  private lazy val tempDirectory = {
    val dir = new File("temp")
    if (!dir.isDirectory) dir.mkdir()
    dir
  }

}