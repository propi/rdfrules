package eu.easyminer.rdf.data.ops

import java.io.File

import eu.easyminer.discretization.DiscretizationTask
import eu.easyminer.discretization.algorithm.Discretization
import eu.easyminer.discretization.impl.Interval
import eu.easyminer.discretization.impl.sorting.{ReversableSortedTraversable, SortedInMemoryNumericTraversable, SortedPersistentNumericTraversable}
import eu.easyminer.discretization.task.{EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask, EquisizeDiscretizationTask}
import eu.easyminer.rdf.data.{Quad, TripleItem}

/**
  * Created by Vaclav Zeman on 26. 2. 2018.
  */
trait Discretizable[Coll] extends QuadsOps[Coll] {

  def discretizeAndGetIntervals(task: DiscretizationTask, mode: Discretizable.DiscretizationMode = Discretizable.DiscretizationMode.InMemory)(f: Quad => Boolean): Array[Interval] = {
    def makeSortedTraversable(col: Traversable[Double])(f: ReversableSortedTraversable[Double] => Array[Interval]): Array[Interval] = mode match {
      case Discretizable.DiscretizationMode.InMemory => f(SortedInMemoryNumericTraversable(col, task.getBufferSize))
      case Discretizable.DiscretizationMode.OnDisc => SortedPersistentNumericTraversable(col, Discretizable.tempDirectory, task.getBufferSize)(f)
    }

    val dis = Discretization[Double](task)
    val col = quads.filter(f).map(_.triple.`object`).collect {
      case TripleItem.NumberDouble(x) => x
    }
    task match {
      case _: EquidistanceDiscretizationTask => dis.discretize(col)
      case _: EquifrequencyDiscretizationTask | _: EquisizeDiscretizationTask => makeSortedTraversable(col)(rst => dis.discretize(rst))
      case _ => Array.empty[Interval]
    }
  }

  def discretize(task: DiscretizationTask, mode: Discretizable.DiscretizationMode = Discretizable.DiscretizationMode.InMemory)(f: Quad => Boolean): Coll = {
    lazy val intervals = discretizeAndGetIntervals(task, mode)(f)
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

  sealed trait DiscretizationMode

  object DiscretizationMode {

    case object InMemory extends DiscretizationMode

    case object OnDisc extends DiscretizationMode

  }

}