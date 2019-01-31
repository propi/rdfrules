package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.utils.CommonValidators.{NonEmpty, RegExp}
import com.thoughtworks.binding.Binding.Constants

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
object Pattern {

  private def variableText: Property = new FixedText[String]("value", "Value", description = "In this position there must be a specified variable. The variable is just one character from a to z.", validator = RegExp("[a-z]"))

  private def constantText: Property = new FixedText[String]("value", "Value", description = "The constant has form as a triple item. It can be resource, text, number, boolean or interval. RESOURCE: <...> or prefix:localName, TEXT: \\\"...\\\", NUMBER: 0, BOOLEAN: true|false, INTERVAL: (x,y) or [x,y]", validator = NonEmpty)

  private def atomItemPatternProperties = {
    val value = new DynamicElement(Constants(
      variableText,
      constantText,
      new DynamicGroup("value", "Values", () => {
        val innerValue = new DynamicElement(Constants(
          variableText,
          constantText
        ))
        Constants(
          new Select("name", "Name", Constants("AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable"), onSelect = {
            case "Variable" => innerValue.setElement(0)
            case "Constant" => innerValue.setElement(1)
            case _ => innerValue.setElement(-1)
          }),
          innerValue
        )
      })
    ))
    Constants(
      new Select("name", "Name", Constants("Any" -> "Any", "AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable", "OneOf" -> "OneOf", "NoneOf" -> "NoneOf"), Some("Any"), {
        case "Variable" => value.setElement(0)
        case "Constant" => value.setElement(1)
        case "OneOf" | "NoneOf" => value.setElement(2)
        case _ => value.setElement(-1)
      }),
      value
    )
  }

  private def atomPatternProperties = Constants(
    new Group("subject", "Subject", atomItemPatternProperties),
    new Group("predicate", "Predicate", atomItemPatternProperties),
    new Group("object", "Object", atomItemPatternProperties),
    new Group("graph", "Graph", atomItemPatternProperties)
  )

  def apply() = Constants(
    new Group("head", "Head", atomPatternProperties, description = "Atom pattern for the head of a rule."),
    new DynamicGroup("body", "Body", () => atomPatternProperties, description = "Atom patterns for body of a rule."),
    new Checkbox("exact", "Exact", description = "If this field is checked then rules must match exactly this pattern. Otherwise, the partial matching is applied; that means rules must match the pattern but there may be additional atoms in the rule (the rule length may be greater than the pattern length).")
  )

}
