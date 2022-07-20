package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterRules(fromOperation: Operation, val info: OperationInfo) extends Operation {
  //val info: OperationInfo = OperationInfo.FilterRules
  val properties: Constants[Property] = Constants(
    Pattern("patterns", "Patterns"),
    DynamicGroup("measures", "Measures")(implicit context => Constants(
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
      new FixedText[String]("value", "Value", validator = RegExp("\\d+(\\.\\d+)?|[><]=? \\d+(\\.\\d+)?|[\\[\\(]\\d+(\\.\\d+)?;\\d+(\\.\\d+)?[\\]\\)]"))
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}