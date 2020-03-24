package com.github.propi.rdfrules.data

import eu.easyminer.discretization
import eu.easyminer.discretization.{AbsoluteSupport, RelativeSupport, Support}
import eu.easyminer.discretization.task.{EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask, EquisizeDiscretizationTask, EquisizeTreeDiscretizationTask}

/**
  * Created by Vaclav Zeman on 4. 8. 2018.
  */
sealed trait DiscretizationTask extends discretization.DiscretizationTask

object DiscretizationTask {

  sealed trait Mode

  object Mode {

    case object InMemory extends Mode

    case object External extends Mode

  }

  case class Equidistance(bins: Int) extends DiscretizationTask with EquidistanceDiscretizationTask {
    def getNumberOfBins: Int = bins

    def getBufferSize: Int = 15000000
  }

  case class Equifrequency(bins: Int, buffer: Int = 15000000, mode: Mode = Mode.External) extends DiscretizationTask with EquifrequencyDiscretizationTask {
    def getNumberOfBins: Int = bins

    def getBufferSize: Int = buffer
  }

  case class Equisize(support: Double, buffer: Int = 15000000, mode: Mode = Mode.External) extends DiscretizationTask with EquisizeDiscretizationTask {
    lazy val getMinSupport: Support = if (support >= 0 && support <= 1) {
      new RelativeSupport(support)
    } else {
      new AbsoluteSupport(support.toInt)
    }

    def getBufferSize: Int = buffer
  }

  case class EquisizeTree(support: Double, arity: Int = 2, buffer: Int = 15000000) extends DiscretizationTask with EquisizeTreeDiscretizationTask {
    lazy val getMinSupport: Support = if (support >= 0 && support <= 1) {
      new RelativeSupport(support)
    } else {
      new AbsoluteSupport(support.toInt)
    }

    def getArity: Int = arity

    def inParallel(): Boolean = true

    def getBufferSize: Int = buffer
  }

}