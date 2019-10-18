package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new DynamicGroup("thresholds", "Thresholds", () => {
      val value = new DynamicElement(Constants(
        new FixedText[Double]("value", "Value", default = "0.1", description = "The minimal value is 0.001 and maximal value is 1.", validator = GreaterThanOrEqualsTo(0.001).map[String] & LowerThanOrEqualsTo(1.0).map[String]),
        new FixedText[Double]("value", "Value", description = "The minimal value is 1.", validator = GreaterThanOrEqualsTo[Int](1)),
        new FixedText[Double]("value", "Value", description = "The minimal value is 2.", validator = GreaterThanOrEqualsTo[Int](2))
      ))
      Constants(
        new Select(
          "name",
          "Name",
          Constants("MinHeadSize" -> "Min head size", "MinHeadCoverage" -> "Min head coverage", "MinSupport" -> "Min support", "MaxRuleLength" -> "Max rule length", "TopK" -> "Top-k", "Timeout" -> "Timeout"),
          onSelect = {
            case "MinHeadCoverage" => value.setElement(0)
            case "MinHeadSize" | "MinSupport" | "TopK" | "Timeout" => value.setElement(1)
            case "MaxRuleLength" => value.setElement(2)
            case _ => value.setElement(-1)
          }
        ),
        value
      )
    }, description = "Mining thresholds. For one mining task you can specify several thresholds. All mined rules must reach defined thresholds. This greatly affects the mining time. Default thresholds are MinHeadSize=100, MinHeadCoverage=0.01, MaxRuleLength=3."),
    new DynamicGroup("patterns", "Patterns", () => Pattern(), description = "In this property, you can define several rule patterns. During the mining phase, each rule must match at least one of the defined patterns."),
    new DynamicGroup("constraints", "Constraints", () => {
      val value = new DynamicElement(Constants(
        new ArrayElement("values", "Values", () => new OptionalText[String]("value", "Value", validator = RegExp("<.*>|\\w+:.*")), description = "List of predicates. You can use prefixed URI or full URI in angle brackets.")
      ))
      Constants(
        new Select("name", "Name", Constants("WithInstances" -> "With instances", "WithInstancesOnlyObjects" -> "With instances at the object position", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case "OnlyPredicates" | "WithoutPredicates" => value.setElement(0)
          case _ => value.setElement(-1)
        }),
        value
      )
    }, description = "Within constraints you can specify whether to mine rules with instances or you can include only chosen predicates into the mining phase.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}