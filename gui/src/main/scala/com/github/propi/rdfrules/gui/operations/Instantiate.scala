package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{Rule, Select}
import com.github.propi.rdfrules.gui.results.Rules
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Instantiate(fromOperation: Operation, val info: OperationInfo) extends Operation {
  private val rule = new Rule
  val properties: Constants[Property] = Constants(
    rule,
    new Select("part", "Part", Constants(
      "Whole" -> "Whole rule",
      "Head" -> "Head",
      "Body" -> "Body"), Some("Whole"), description = "The part of the rule to be instantiated.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  def setRule(rule: Rules.Rule): Unit = this.rule.setRule(rule)
}