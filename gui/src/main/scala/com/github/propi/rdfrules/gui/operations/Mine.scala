package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val thresholds = new DynamicGroup("thresholds", "Thresholds", () => {
      val value = new DynamicElement(Constants(
        new FixedText[Double]("value", "Value", default = "0.1", description = "The minimal value is 0.001 and maximal value is 1.", validator = GreaterThanOrEqualsTo(0.001).map[String] & LowerThanOrEqualsTo(1.0).map[String]),
        new FixedText[Double]("value", "Value", description = "The minimal value is 1.", validator = GreaterThanOrEqualsTo[Int](1)),
        new FixedText[Double]("value", "Value", description = "The minimal value is 2.", validator = GreaterThanOrEqualsTo[Int](2)),
        new FixedText[Double]("value", "Value", description = "If negative value, the minimal atom size is same as the current minimal support threshold.", default = "-1")
      ))
      Constants(
        new Select(
          "name",
          "Name",
          Constants("MinHeadSize" -> "Min head size", "MinAtomSize" -> "Min atom size", "MinHeadCoverage" -> "Min head coverage", "MinSupport" -> "Min support", "MaxRuleLength" -> "Max rule length", "TopK" -> "Top-k", "Timeout" -> "Timeout"),
          onSelect = {
            case "MinHeadCoverage" => value.setElement(0)
            case "MinHeadSize" | "MinSupport" | "TopK" | "Timeout" => value.setElement(1)
            case "MaxRuleLength" => value.setElement(2)
            case "MinAtomSize" => value.setElement(3)
            case _ => value.setElement(-1)
          }
        ),
        value
      )
    }, description = "Mining thresholds. For one mining task you can specify several thresholds. All mined rules must reach defined thresholds. This greatly affects the mining time. Default thresholds are MinHeadSize=100, MinSupport=1, MaxRuleLength=3.")
    val constraints = new DynamicGroup("constraints", "Constraints", () => {
      val value = new DynamicElement(Constants(
        new ArrayElement("values", "Values", () => new OptionalText[String]("value", "Value", validator = RegExp("<.*>|\\w+:.*")), description = "List of predicates. You can use prefixed URI or full URI in angle brackets.")
      ))
      Constants(
        new Select("name", "Name", Constants("WithoutConstants" -> "Without constants", "OnlyObjectConstants" -> "With constants at the object position", "OnlySubjectConstants" -> "With constants at the subject position", "OnlyLeastFunctionalConstants" -> "With constants at the least functional position", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case "OnlyPredicates" | "WithoutPredicates" => value.setElement(0)
          case _ => value.setElement(-1)
        }),
        value
      )
    }, description = "Within constraints you can specify whether to mine rules without constants or you can include only chosen predicates into the mining phase.")
    thresholds.setValue(js.Array(
      js.Dictionary("name" -> "MinHeadCoverage", "value" -> 0.01),
      js.Dictionary("name" -> "Timeout", "value" -> 5)
    ).asInstanceOf[js.Dynamic])
    constraints.setValue(js.Array(js.Dictionary("name" -> "WithoutConstants")).asInstanceOf[js.Dynamic])
    Constants(
      thresholds,
      new DynamicGroup("patterns", "Patterns", () => Pattern(), description = "In this property, you can define several rule patterns. During the mining phase, each rule must match at least one of the defined patterns."),
      constraints
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}