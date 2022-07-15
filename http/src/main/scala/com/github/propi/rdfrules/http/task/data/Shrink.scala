package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.data.Shrink.ShrinkSetup
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Shrink(shrinkSetup: ShrinkSetup) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = Shrink

  def execute(input: Dataset): Dataset = shrinkSetup match {
    case ShrinkSetup.Drop(n) => input.drop(n)
    case ShrinkSetup.Take(n) => input.take(n)
    case ShrinkSetup.Slice(from, until) => input.slice(from, until)
  }
}

object Shrink extends TaskDefinition {
  val name: String = "ShrinkQuads"

  sealed trait ShrinkSetup

  object ShrinkSetup {
    case class Drop(n: Int) extends ShrinkSetup

    case class Take(n: Int) extends ShrinkSetup

    case class Slice(from: Int, until: Int) extends ShrinkSetup
  }
}