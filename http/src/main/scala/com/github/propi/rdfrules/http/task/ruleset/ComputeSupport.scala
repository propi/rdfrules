package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.ruleset.ComputeConfidence.ConfidenceType
import com.github.propi.rdfrules.http.task.ruleset.ComputeSupport.SupportType
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ComputeSupport(supportType: SupportType)(implicit debugger: Debugger) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = ComputeSupport

  def execute(input: Ruleset): Ruleset = supportType match {
    case SupportType.Support(min) => input.computeSupport(min)
    case SupportType.HeadCoverage(min) => input.computeHeadCoverage(min)
  }
}

object ComputeSupport extends TaskDefinition {
  val name: String = "ComputeSupport"

  sealed trait SupportType

  object SupportType {
    case class Support(value: Int) extends SupportType

    case class HeadCoverage(value: Double) extends SupportType
  }
}