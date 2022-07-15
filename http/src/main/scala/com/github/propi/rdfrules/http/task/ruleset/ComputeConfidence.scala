package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.ruleset.ComputeConfidence.ConfidenceType
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ComputeConfidence(confidenceType: ConfidenceType)(implicit debugger: Debugger) extends Task[Ruleset, Ruleset] {
  val companion: TaskDefinition = ComputeConfidence

  def execute(input: Ruleset): Ruleset = confidenceType match {
    case ConfidenceType.StandardConfidence(min, topK) => input.computeConfidence(min, topK)
    case ConfidenceType.PcaConfidence(min, topK) => input.computeConfidence(min, topK)
    case ConfidenceType.Lift(min) => input.computeLift(min)
  }
}

object ComputeConfidence extends TaskDefinition {
  val name: String = "ComputeConfidence"

  sealed trait ConfidenceType

  object ConfidenceType {
    case class StandardConfidence(min: Double, topK: Int) extends ConfidenceType

    case class PcaConfidence(min: Double, topK: Int) extends ConfidenceType

    case class Lift(min: Double) extends ConfidenceType
  }
}