package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterRules(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.FilterRules
  val properties: Constants[Property] = Constants(
    new DynamicGroup("patterns", "Patterns", () => Pattern(), description = "In this property, you can define several rule patterns. During the filtering phase, each rule must match at least one of the defined patterns."),
    new DynamicGroup("measures", "Measures", () => Constants(
      new Select("name", "Name", Constants(
        "RuleLength" -> "Rule length",
        "HeadSize" -> "Head size",
        "Support" -> "Support",
        "HeadCoverage" -> "Head coverage",
        "BodySize" -> "Body size",
        "Confidence" -> "Confidence",
        "PcaConfidence" -> "PCA confidence",
        "PcaBodySize" -> "PCA body size",
        "HeadConfidence" -> "Head confidence",
        "Lift" -> "Lift",
        "Cluster" -> "Cluster"
      )),
      new FixedText[String]("value", "Value", description = "Some condition for numerical comparison, e.g, '> 0.5' or '(0.8;0.9]' or '1.0'", validator = RegExp("\\d+(\\.\\d+)?|[><]=? \\d+(\\.\\d+)?|[\\[\\(]\\d+(\\.\\d+)?;\\d+(\\.\\d+)?[\\]\\)]"))
    ), description = "Rules filtering by their interest measures values.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}