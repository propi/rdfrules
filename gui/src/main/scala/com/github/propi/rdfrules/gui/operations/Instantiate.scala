package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{Checkbox, Rule, Select}
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
      "Whole" -> "Whole rule (correct predictions)",
      "Head" -> "Head (head support, or head size - if two variables)",
      "HeadExisting" -> "Head + Existing (support)",
      "HeadMissing" -> "Head + Missing (without support)",
      "BodyAll" -> "Body + All (body size)",
      "BodyExisting" -> "Body + Existing (correct predictions)",
      "BodyMissing" -> "Body + Missing (incorrect predictions)",
      "BodyComplementary" -> "Body + Complementary (PCA incorrect predictions)"), Some("Whole"), description = "The part of the rule to be instantiated. If the body is instantiated, there are four options. Existing: all instantiated triples in the head are contained in the input KG. Missing: all instantiated triples in the head are not contained in the input KG. All: all instantiated triples in the head are Existing or Missing. Complementary: an instantiated triple in the head is Missing and the subject did not contain any information related with the predicted predicate (it is new valuable knowledge)."),
    new Checkbox("allowDuplicateAtoms", "Allow duplicate atoms", description = "Check this if you want to enable duplicate instantiated atoms (triples) during projections mapping.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  def setRule(rule: Rules.Rule): Unit = this.rule.setRule(rule)
}