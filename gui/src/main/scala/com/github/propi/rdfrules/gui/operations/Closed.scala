package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.Select
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Closed(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new Select("measure", "Measure", Constants(
      "RuleLength" -> "Rule length",
      "HeadSize" -> "Head size",
      "Support" -> "Support",
      "HeadCoverage" -> "Head coverage",
      "BodySize" -> "Body size",
      "Confidence" -> "Confidence",
      "PcaConfidence" -> "PCA confidence",
      "PcaBodySize" -> "PCA body size",
      "HeadConfidence" -> "Head confidence",
      "Lift" -> "Lift"
    ), Some("HeadCoverage"), description = "Some measure by which to do this pruning strategy.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}