package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.Mine
  val properties: Constants[Property] = Constants(
    new DynamicGroup("thresholds", "Thresholds", () => Constants(
      new Select("name", "Name", Constants("MinHeadSize" -> "Min head size", "MinHeadCoverage" -> "Min head coverage", "MaxRuleLength" -> "Max rule length", "TopK" -> "Top-k", "Timeout" -> "Timeout")),
      new FixedText[Double]("value", "Value")
    )),
    new DynamicGroup("patterns", "Patterns", () => Pattern()),
    new DynamicGroup("constraints", "Constraints", () => {
      val value = new DynamicElement(Constants(
        new ArrayElement("values", "Values", () => new OptionalText[String]("value", "Value"))
      ))
      Constants(
        new Select("name", "Name", Constants("WithInstances" -> "With instances", "WithInstancesOnlyObjects" -> "With instances at the object position", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case "OnlyPredicates" | "WithoutPredicates" => value.setElement(0)
          case _ => value.setElement(-1)
        }),
        value
      )
    })
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}